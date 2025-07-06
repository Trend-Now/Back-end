package com.trend_now.backend.search.aplication;

import static com.trend_now.backend.board.application.BoardRedisService.BOARD_KEY_DELIMITER;

import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.cache.RealTimeBoardCache;
import com.trend_now.backend.board.dto.RealtimeBoardListDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.post.dto.PostListResponseDto;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.dto.RealtimePostSearchDto;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.search.dto.AutoCompleteDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostsRepository postsRepository;
    private final RealTimeBoardCache realTimeBoardCache;
    private final BoardRepository boardRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 검색어에 따른 실시간 인기 게시판 조회
     */
    public List<RealtimeBoardListDto> findRealtimeBoardsByKeyword(String keyword) {
        Map<Long, BoardCacheEntry> boardCacheEntryMap = realTimeBoardCache.getBoardCacheEntryMap()
            .asMap();
        // 검색어가 포함된 게시판 목록 필터링
        List<Long> filteredBoardIds = boardCacheEntryMap.entrySet().stream()
            .filter(entry -> entry.getValue().getBoardName().contains(keyword))
            .map(Entry::getKey)
            .toList();

        List<RealtimeBoardListDto> realtimeBoardList = boardRepository.findRealtimeBoardsByIds(filteredBoardIds);
        // 실시간 게시판 만료 시간 데이터 DTO에 추가
        realtimeBoardList.forEach(board -> {
            String key = board.getBoardName() + BOARD_KEY_DELIMITER + board.getBoardId();
            board.setBoardLiveTime(redisTemplate.getExpire(key));
        });

        return realtimeBoardList;
    }

    /**
     * 검색어에 따른 실시간 인기 게시판의 게시글 조회
     */
    public RealtimePostSearchDto findRealtimePostsByKeyword(String keyword, int page, int size) {
        // 캐싱된 실시간 게시판 목록 조회
        Map<Long, BoardCacheEntry> boardCacheEntryMap = realTimeBoardCache.getBoardCacheEntryMap()
            .asMap();
        // 작성 날짜 기준 최신순, 작성 날짜가 값은 값은 수정 날짜 기준 최신순으로 정렬
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("updatedAt")));

        Set<Long> boardIds = boardCacheEntryMap.keySet();
        Page<PostWithBoardSummaryDto> postWithBoardSummaryList = postsRepository.findByKeywordAndRealTimeBoard(
            keyword, boardIds, pageable);
        return RealtimePostSearchDto.of(
            postWithBoardSummaryList.getTotalPages(), postWithBoardSummaryList.getTotalElements(),
            postWithBoardSummaryList.getContent());

    }

    /**
     * 캐싱 되어 있는 고정 게시판 목록에 속한 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글을 조회합니다.
     */
    public Map<String, PostListResponseDto> findFixedPostsByKeyword(String keyword, int page,
        int size) {
        // 캐싱 돼 있는 고정 게시판 목록 조회
        Map<Long, BoardCacheEntry> fixedBoardCacheMap = realTimeBoardCache.getFixedBoardCacheMap()
            .asMap();
        // 작성 날짜 기준 최신순, 작성 날짜가 값은 값은 수정 날짜 기준 최신순으로 정렬
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("updatedAt")));

        // 결과로 반환할 비어있는 Map 생성
        Map<String, PostListResponseDto> fixBoardList = new HashMap<>();
        // 고정 게시판 개수만큼 반복하며 fixBoardList에 조회된 게시글 추가
        for (Long fixBoardId : fixedBoardCacheMap.keySet()) {
            // fixBoard에 속한 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글 조회
            Page<PostSummaryDto> postSummaryDtoList = postsRepository.findByFixBoardsAndKeyword(
                keyword, fixBoardId, pageable);
            // 게시판 이름을 Key 값으로 사용
            String boardName = fixedBoardCacheMap.get(fixBoardId).getBoardName();
            // {고정게시판이름 : [게시글 리스트]} 형식
            fixBoardList.put(boardName,
                PostListResponseDto.of(boardName + " 게시글 검색 결과",
                    postSummaryDtoList.getTotalPages(),
                    postSummaryDtoList.getTotalElements(),
                    postSummaryDtoList.getContent()));
        }
        return fixBoardList;
    }

    // 게시판 이름 자동완성 메서드
    public List<AutoCompleteDto> findBoardsByPrefix(String prefix) {
        // 공백 제거
        String trimmedPrefix = prefix.replaceAll(" ", "");
        // 캐싱해놓은 실시간 인기 검색어 리스트 조회
        Map<Long, BoardCacheEntry> boardCacheEntryMap = realTimeBoardCache.getBoardCacheEntryMap()
            .asMap();
        // 캐싱해놓은 고정 게시판 리스트 조회
        Map<Long, BoardCacheEntry> fixedBoardCacheMap = realTimeBoardCache.getFixedBoardCacheMap()
            .asMap();

        List<AutoCompleteDto> filteredBoards = boardCacheEntryMap.entrySet().stream()
            .filter(fixedBoard -> fixedBoard.getValue().getBoardName()
                .contains(trimmedPrefix))
            .map(fixedBoard -> AutoCompleteDto.builder()
                .boardId(fixedBoard.getKey())
                .boardName(fixedBoard.getValue().getBoardName())
                .build())
            .toList();

        // 고정 게시판 조회
        List<AutoCompleteDto> fixedBoardList = fixedBoardCacheMap.entrySet().stream()
            .filter(fixedBoard -> fixedBoard.getValue().getBoardName()
                .contains(trimmedPrefix))
            .map(fixedBoard -> AutoCompleteDto.builder()
                .boardId(fixedBoard.getKey())
                .boardName(fixedBoard.getValue().getBoardName())
                .build())
            .toList();

        // 실시간 게시판과 고정 게시판 결합
        List<AutoCompleteDto> result = new ArrayList<>(filteredBoards);
        result.addAll(fixedBoardList);

        return result;
    }

}
