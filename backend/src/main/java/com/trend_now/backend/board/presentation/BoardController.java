package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
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
public class BoardController {

    private final BoardRedisService boardRedisService;

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
