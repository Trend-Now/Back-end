package com.trend_now.backend.search.aplication;

import static com.trend_now.backend.board.application.BoardRedisService.BOARD_KEY_DELIMITER;

import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.application.PostViewService;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.search.dto.FixedPostSearchDto;
import com.trend_now.backend.search.dto.RealtimePostSearchDto;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.search.dto.AutoCompleteDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
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
    private final BoardCache boardCache;
    private final BoardRepository boardRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PostLikesService postLikesService;
    private final PostViewService postViewService;

    /**
     * 검색어에 따른 실시간 인기 게시판 조회
     */
    public List<RealtimeBoardDto> findRealtimeBoardsByKeyword(String keyword) {
        Map<Long, BoardCacheEntry> boardCacheEntryMap = boardCache.getBoardCacheEntryMap()
            .asMap();
        // 검색어가 포함된 게시판 목록 필터링
        List<Long> filteredBoardIds = boardCacheEntryMap.entrySet().stream()
            .filter(entry -> entry.getValue().getBoardName().contains(keyword))
            .map(Entry::getKey)
            .toList();

        // 검색될 경우 게시판에 대한 총 조회수를 보여줘야 하기 때문에 강제로 Redis와 DB를 동기화
        postViewService.syncViewCountToDatabase();
        List<RealtimeBoardDto> realtimeBoardList = boardRepository.findRealtimeBoardsByIds(
            filteredBoardIds);

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
        Map<Long, BoardCacheEntry> boardCacheEntryMap = boardCache.getBoardCacheEntryMap()
            .asMap();
        // 작성 날짜 기준 최신순, 작성 날짜가 값은 값은 수정 날짜 기준 최신순으로 정렬
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("updatedAt")));

        Set<Long> boardIds = boardCacheEntryMap.keySet();
        Page<PostWithBoardSummaryDto> postWithBoardSummaryList = postsRepository.findByKeywordAndRealTimeBoard(
            keyword, boardIds, pageable);
        postWithBoardSummaryList.forEach(postWithBoardSummary -> {
            // 만약 redis에 저장된 게시글 조회수와 게시글 좋아요 수가 있다면, 해당 조회수를 PostSummaryDto에 설정 (Look Aside)
            int postViewCount = postViewService.getPostViewCount(postWithBoardSummary.getPostId());
            postWithBoardSummary.setViewCount(postViewCount);
            int postLikesCount = postLikesService.getPostLikesCount(
                postWithBoardSummary.getBoardId(), postWithBoardSummary.getPostId());
            postWithBoardSummary.setLikeCount(postLikesCount);
        });
        return RealtimePostSearchDto.of(
            postWithBoardSummaryList.getTotalPages(), postWithBoardSummaryList.getTotalElements(),
            postWithBoardSummaryList.getContent());

    }

    /**
     * 캐싱 되어 있는 고정 게시판 목록에 속한 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글을 조회합니다.
     */
    public FixedPostSearchDto findFixedPostsByKeyword(String keyword, Long boardId, int page,
        int size) {
        ConcurrentMap<Long, BoardCacheEntry> fixedBoardCacheMap = boardCache.getFixedBoardCacheMap()
            .asMap();
        if (!fixedBoardCacheMap.containsKey(boardId)) {
            log.error("{}은 고정게시판 목록에 존재하지 않는 아이디입니다.", boardId);
            throw new NotFoundException("해당 게시판이 고정게시판 목록에 존재하지 않습니다.");
        }
        // 작성 날짜 기준 최신순, 작성 날짜가 값은 값은 수정 날짜 기준 최신순으로 정렬
        Pageable pageable = PageRequest.of(page - 1, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("updatedAt")));

        // boardId에 속한 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글 조회
        Page<PostSummaryDto> postSummaryDtoPage = postsRepository.findByFixBoardsAndKeyword(
            keyword, boardId, pageable);

        postSummaryDtoPage.forEach(postSummaryDto -> {
            // 만약 redis에 저장된 게시글 조회수와 게시글 좋아요 수가 있다면, 해당 조회수를 PostSummaryDto에 설정 (Look Aside)
            int postViewCount = postViewService.getPostViewCount(postSummaryDto.getPostId());
            postSummaryDto.setViewCount(postViewCount);
            int postLikesCount = postLikesService.getPostLikesCount(boardId, postSummaryDto.getPostId());
            postSummaryDto.setLikeCount(postLikesCount);
        });

        return FixedPostSearchDto.of(postSummaryDtoPage.getTotalPages(),
            postSummaryDtoPage.getTotalElements(), postSummaryDtoPage.getContent());
    }

    // 게시판 이름 자동완성 메서드
    public List<AutoCompleteDto> findBoardsByPrefix(String prefix) {
        // 공백 제거
        String trimmedPrefix = prefix.replaceAll(" ", "");
        // 캐싱해놓은 실시간 인기 검색어 리스트 조회
        ConcurrentMap<Long, BoardCacheEntry> boardCacheEntryMap = boardCache.getBoardCacheEntryMap()
            .asMap();
        // 캐싱해놓은 고정 게시판 리스트 조회
        ConcurrentMap<Long, BoardCacheEntry> fixedBoardCacheMap = boardCache.getFixedBoardCacheMap()
            .asMap();

        List<AutoCompleteDto> filteredBoards = boardCacheEntryMap.entrySet().stream()
            .filter(fixedBoard -> fixedBoard.getValue().getBoardName().replaceAll(" ", "")
                .contains(trimmedPrefix))
            .map(fixedBoard -> AutoCompleteDto.builder()
                .boardId(fixedBoard.getKey())
                .boardName(fixedBoard.getValue().getBoardName())
                .build())
            .toList();

        // 고정 게시판 조회
        List<AutoCompleteDto> fixedBoardList = fixedBoardCacheMap.entrySet().stream()
            .filter(entry -> entry.getValue().getBoardName().contains(trimmedPrefix))
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
