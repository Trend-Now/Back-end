package com.trend_now.backend.search.presentation;

import com.trend_now.backend.board.dto.BoardAutoCompleteResponseDto;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.search.aplication.SearchService;
import com.trend_now.backend.search.dto.SearchResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@Tag(name = "Search API", description = "검색 관련 API")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "검색어로 게시글 조회", description = "검색어 기반으로 요구사항에 맞게 게시글을 조회합니다.")
    @GetMapping("/posts")
    public ResponseEntity<SearchResponseDto> findAllPostsByBoardId(
        @RequestParam String keyword,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "10") int size) {
        SearchResponseDto response = searchService.findBoardAndPostByKeyword(keyword, page, size);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "검색어 자동완성", description = "게시판 이름 중 prefix가 포함된 게시판이 있으면 해당 리스트를 반환한다.")
    @GetMapping("/auto-complete")
    public ResponseEntity<BoardAutoCompleteResponseDto> autoCompleteBoardName(
        @RequestParam String prefix) {
        List<BoardInfoDto> boardList = searchService.findBoardsByPrefix(prefix);
        BoardAutoCompleteResponseDto response = BoardAutoCompleteResponseDto.from(boardList);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
