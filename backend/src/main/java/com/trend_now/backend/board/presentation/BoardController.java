package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards")
@Tag(name = "Board API", description = "게시판 관련 API")
public class BoardController {

    private final BoardRedisService boardRedisService;

    @Operation(summary = "실시간 게시판 리스트 조회", description = "실시간 게시판 리스트를 페이징하여 가져옵니다.")
    @GetMapping("/list")
    public ResponseEntity<BoardPagingResponseDto> findAllRealTimeBoards(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        BoardPagingRequestDto boardPagingRequestDto = new BoardPagingRequestDto(page, size);
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                boardPagingRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(allRealTimeBoardPaging);
    }
}
