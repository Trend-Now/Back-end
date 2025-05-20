package com.trend_now.backend.search.aplication;

import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.cache.RealTimeBoardCache;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.dto.BoardSummaryDto;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.search.dto.SearchResponseDto;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostsRepository postsRepository;
    private final RealTimeBoardCache realTimeBoardCache;
    private final PostLikesService postLikesService;

    /**
     * <pre>
     * 실시간 인기 게시판
     * 실시간 인기 게시판의 게시글
     * 고정 게시판 고정 게시판의 게시글
     * 순서대로 조회 후 반환하는 메서드
     * </pre>
     */
    public SearchResponseDto findRealTimeBoardByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        // 현재 인메모리에 캐싱된 데이터 조회
        List<BoardCacheEntry> boardCacheEntryList = realTimeBoardCache.getBoardCacheEntryList();
        List<BoardCacheEntry> fixedBoardCacheList = realTimeBoardCache.getFixedBoardCacheList();
        List<Long> boardCacheIdList = realTimeBoardCache.getBoardCacheIdList();

        // 실시간 인기 게시판 중, 키워드가 포함된 게시판만 필터링
        List<BoardSummaryDto> filteredKeywords = filterBoardsByKeyword(keyword, boardCacheEntryList);

        // 실시간 게시판에 해당하는(타이머가 남아있는) 게시글 중, 내용 또는 제목에 keyword가 포함된 게시글 조회
        List<PostSummaryDto> postSummaryDtoList = filterRealTimePostsByKeyword(keyword, boardCacheIdList, pageable);

        // 고정 게시판 조회
        List<BoardSummaryDto> fixedBoardTitleList = filterBoardsByKeyword(keyword, fixedBoardCacheList);

        // 고정 게시판에서 게시글 조회
        List<PostSummaryDto> fixedPostSummaryList = filterFixedPostsByKeyword(keyword, pageable);

        log.info("{} 검색 성공, 결과를 반환합니다", keyword);

        return SearchResponseDto.builder()
            .message("게시글 검색 조회 성공")
            .boardList(filteredKeywords)
            .postList(postSummaryDtoList)
            .fixedBoardList(fixedBoardTitleList)
            .fixedPostList(fixedPostSummaryList)
            .build();
    }

    private List<PostSummaryDto> filterRealTimePostsByKeyword(String keyword, List<Long> boardCacheIdList, Pageable pageable) {
        Page<Posts> posts = postsRepository.findByKeywordAndRealTimeBoard(keyword, boardCacheIdList, pageable);
        return convertToPostSummaryDtos(posts);
    }

    private List<BoardSummaryDto> filterBoardsByKeyword(String keyword,
        List<BoardCacheEntry> BoardCacheEntryList) {
        return BoardCacheEntryList.stream()
            .filter(boardEntry -> boardEntry.getBoardName().contains(keyword))
            .map(
                boardEntry -> BoardSummaryDto.builder()
                    .boardId(boardEntry.getBoardId())
                    .boardName(boardEntry.getBoardName())
                    .createdAt(boardEntry.getCreatedAt())
                    .updatedAt(boardEntry.getUpdatedAt())
                    .build())
            .sorted(Comparator.comparing(BoardSummaryDto::getUpdatedAt).reversed()
                .thenComparing(BoardSummaryDto::getCreatedAt, Comparator.reverseOrder()))
            .toList();
    }

    private List<PostSummaryDto> filterFixedPostsByKeyword(String keyword, Pageable pageable) {

        Page<Posts> fixedPostsPage = postsRepository.findByBoardCategoryAndKeyword(keyword,
            BoardCategory.FIXED, pageable);
        return convertToPostSummaryDtos(fixedPostsPage);
    }

    private List<PostSummaryDto> convertToPostSummaryDtos(Page<Posts> byFixedBoardAndKeyword) {
        return byFixedBoardAndKeyword.getContent().stream()
            .map(post -> {
                int postLikesCount = postLikesService.getPostLikesCount(post.getBoards().getId(),
                    post.getId());
                return PostSummaryDto.of(post, postLikesCount);
            }).toList();
    }

}
