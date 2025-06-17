package com.trend_now.backend.search.aplication;

import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.cache.RealTimeBoardCache;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSummaryDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.dto.PostListResponseDto;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.dto.RealtimePostSearchDto;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.search.dto.AutoCompleteDto;
import com.trend_now.backend.search.dto.SearchResponseDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostsRepository postsRepository;
    private final RealTimeBoardCache realTimeBoardCache;
    private final PostLikesService postLikesService;
    private final BoardRepository boardRepository;

    /**
     * <pre>
     * 실시간 인기 게시판
     * 실시간 인기 게시판의 게시글
     * 고정 게시판 고정 게시판의 게시글
     * 순서대로 조회 후 반환하는 메서드
     * </pre>
     */
    public SearchResponseDto findBoardAndPostByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("updatedAt")));
        // 현재 인메모리에 캐싱된 데이터 조회
        Map<Long, BoardCacheEntry> boardCacheEntryMap = realTimeBoardCache.getBoardCacheEntryMap()
            .asMap();
        Map<Long, BoardCacheEntry> fixedBoardCacheMap = realTimeBoardCache.getFixedBoardCacheMap()
            .asMap();

        // 실시간 인기 게시판 중, 키워드가 포함된 게시판만 필터링
        List<BoardSummaryDto> filteredKeywords = filterBoardsByKeyword(keyword, boardCacheEntryMap);

        // 실시간 게시판에 해당하는(타이머가 남아있는) 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글 조회
        Page<PostWithBoardSummaryDto> postWithBoardSummaryList = filterRealTimePostsByKeyword(
            keyword, boardCacheEntryMap.keySet(), pageable);
        RealtimePostSearchDto realtimePostSearchDto = RealtimePostSearchDto.of(
            postWithBoardSummaryList.getTotalPages(), postWithBoardSummaryList.getTotalElements(),
            postWithBoardSummaryList.getContent());

        // 고정 게시판 조회
        List<BoardSummaryDto> fixedBoardTitleList = filterBoardsByKeyword(keyword,
            fixedBoardCacheMap);

        // 고정 게시판에서 게시글 조회
        Map<String, PostListResponseDto> fixPostSearchMap = filterFixedPostsByKeyword(
            keyword,
            fixedBoardCacheMap, pageable);

        log.info("{} 검색 성공, 결과를 반환합니다", keyword);

        return SearchResponseDto.builder()
            .message("게시글 검색 조회 성공")
            .realtimeBoardList(filteredKeywords)
            .realtimePostList(realtimePostSearchDto)
            .fixedBoardList(fixedBoardTitleList)
            .fixedPostList(fixPostSearchMap)
            .build();
    }

    private Page<PostWithBoardSummaryDto> filterRealTimePostsByKeyword(String keyword,
        Set<Long> boardCacheIdList, Pageable pageable) {
        return postsRepository.findByKeywordAndRealTimeBoard(keyword, boardCacheIdList,
            pageable);
    }

    private List<BoardSummaryDto> filterBoardsByKeyword(String keyword,
        Map<Long, BoardCacheEntry> boardCacheEntryMap) {
        List<Boards> boardList = boardRepository.findByIdIn(boardCacheEntryMap.keySet());
        return boardList.stream()
            .filter(boardEntry -> boardEntry.getName().contains(keyword))
            .map(
                boardEntry -> BoardSummaryDto.builder()
                    .boardId(boardEntry.getId())
                    .boardName(boardEntry.getName())
                    .createdAt(boardEntry.getCreatedAt())
                    .updatedAt(boardEntry.getUpdatedAt())
                    .build())
            .sorted(Comparator.comparing(BoardSummaryDto::getUpdatedAt).reversed()
                .thenComparing(BoardSummaryDto::getCreatedAt, Comparator.reverseOrder()))
            .toList();
    }

    private Map<String, PostListResponseDto> filterFixedPostsByKeyword(String keyword,
        Map<Long, BoardCacheEntry> fixedBoardCacheMap, Pageable pageable) {

        Map<String, PostListResponseDto> fixBoardList = new HashMap<>();
        /**
         * 캐싱 되어 있는 고정 게시판 목록에 속한 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글을 조회합니다.
         */
        for (Long fixBoardId : fixedBoardCacheMap.keySet()) {
            // fixBoard에 속한 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글 조회
            Page<PostSummaryDto> postSummaryDtoList = postsRepository.findByFixBoardsAndKeyword(
                keyword, fixBoardId, pageable);
            String boardName = fixedBoardCacheMap.get(fixBoardId).getBoardName();
            fixBoardList.put(
                boardName,
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
