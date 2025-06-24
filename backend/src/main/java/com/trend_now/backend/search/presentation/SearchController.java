package com.trend_now.backend.search.presentation;

import com.trend_now.backend.board.dto.BoardSummaryDto;
import com.trend_now.backend.post.dto.PostListResponseDto;
import com.trend_now.backend.post.dto.RealtimePostSearchDto;
import com.trend_now.backend.search.aplication.SearchService;
import com.trend_now.backend.search.dto.AutoCompleteDto;
import com.trend_now.backend.search.dto.SearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@Tag(name = "Search API", description = "검색 관련 API")
public class SearchController {

    public static final String REALTIME_BOARD_SEARCH_SUCCESS = "실시간 게시판 목록 검색 완료";
    public static final String REALTIME_POST_SEARCH_SUCCESS = "실시간 게시판의 게시글 목록 검색 완료";
    public static final String FIXED_POST_SEARCH_SUCCESS = "고정 게시판의 게시글 목록 검색 완료";

    private final SearchService searchService;

    @Operation(summary = "검색어에 따른 실시간 게시판 목록 조회", description = "검색어에 해당하는 게시판 목록을 조회합니다.")
    @GetMapping("/realtimeBoards")
    public ResponseEntity<SearchResponseDto> findRealtimeBoards(@RequestParam String keyword) {
        List<BoardSummaryDto> realtimeBoardsByKeyword = searchService.findRealtimeBoardsByKeyword(
            keyword);

        log.info("실시간 게시판 목록 검색 완료, 검색어: {}", keyword);

        return ResponseEntity.status(HttpStatus.OK)
            .body(SearchResponseDto.of(REALTIME_BOARD_SEARCH_SUCCESS, realtimeBoardsByKeyword));
    }

    @Operation(summary = "검색어에 따른 실시간 게시판의 게시글 조회", description = "검색어에 해당하는 실시간 게시판의 게시글 목록을 조회합니다.")
    @GetMapping("/realtimePosts")
    public ResponseEntity<SearchResponseDto> findRealtimePosts(
        @RequestParam String keyword,
        @RequestParam(required = false, defaultValue = "1") int page,
        @RequestParam(required = false, defaultValue = "10") int size) {
        RealtimePostSearchDto realtimePostsByKeyword = searchService.findRealtimePostsByKeyword(
            keyword, page, size);

        log.info("실시간 게시판의 게시글 목록 검색 완료, 검색어: {}", keyword);

        return ResponseEntity.status(HttpStatus.OK)
            .body(SearchResponseDto.of(REALTIME_POST_SEARCH_SUCCESS, realtimePostsByKeyword));
    }

    @Operation(summary = "검색어에 따른 고정 게시판의 게시글 조회", description = "검색어에 해당하는 고정 게시판의 게시글 목록을 조회합니다.")
    @GetMapping("/fixedPosts")
    public ResponseEntity<SearchResponseDto> findFixedPosts(
        @RequestParam String keyword,
        @RequestParam(required = false, defaultValue = "1") int page,
        @RequestParam(required = false, defaultValue = "10") int size) {

        Map<String, PostListResponseDto> fixedPostsByKeyword = searchService.findFixedPostsByKeyword(
            keyword, page, size);

        log.info("고정 게시판의 게시글 목록 검색 완료, 검색어: {}", keyword);

        return ResponseEntity.status(HttpStatus.OK)
            .body(SearchResponseDto.of(FIXED_POST_SEARCH_SUCCESS, fixedPostsByKeyword));
    }

    @Operation(summary = "검색어 자동완성", description = "게시판 이름 중 prefix가 포함된 게시판이 있으면 해당 리스트를 반환한다.")
    @GetMapping("/auto-complete")
    public ResponseEntity<List<AutoCompleteDto>> autoCompleteBoardName(
        @RequestParam String keyword) {
        List<AutoCompleteDto> boardList = searchService.findBoardsByPrefix(keyword);
        return ResponseEntity.status(HttpStatus.OK).body(boardList);
    }
}
