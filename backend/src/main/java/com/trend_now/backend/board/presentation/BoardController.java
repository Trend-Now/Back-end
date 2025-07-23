package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.FixedBoardSaveDto;
import com.trend_now.backend.board.dto.RealtimeBoardListDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards")
@Tag(name = "Board API", description = "게시판 관련 API")
public class BoardController {

    private final BoardRedisService boardRedisService;
    private final BoardService boardService;

    @Operation(summary = "실시간 게시판 리스트 조회", description = "실시간 게시판 리스트를 페이징하여 가져옵니다.")
    @GetMapping("/list")
    public ResponseEntity<BoardPagingResponseDto> findAllRealTimeBoards(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        BoardPagingRequestDto boardPagingRequestDto = new BoardPagingRequestDto(page - 1, size);
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                boardPagingRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(allRealTimeBoardPaging);
    }

    @Operation(summary = "고정 게시판 리스트 조회", description = "고정 게시판 리스트를 가져옵니다.")
    @GetMapping("/fixedList")
    public ResponseEntity<List<RealtimeBoardListDto>> findAllFixedBoards() {
        List<RealtimeBoardListDto> fixedBoardList = boardService.getFixedBoardList();

        return ResponseEntity.status(HttpStatus.OK).body(fixedBoardList);
    }

    @Operation(summary = "고정 게시판 추가", description = "고정 게시판 항목을 추가합니다.")
    @PostMapping("/fixed")
    public ResponseEntity<String> addFixedBoard(@RequestBody FixedBoardSaveDto fixedBoardSaveDto) {
        boardService.addFixedBoard(fixedBoardSaveDto);
        return ResponseEntity.status(HttpStatus.OK).body("고정 게시판 항목이 추가되었습니다.");
    }

    @Operation(summary = "실시간 게시판 정보 반환", description = "실시간 게시판 입장 시 필요한 정보를 반환합니다.")
    @GetMapping("/realtime")
    public ResponseEntity<BoardInfoDto> getBoardInfo(
            @RequestParam Long boardId) {
        BoardInfoDto boardInfo = boardRedisService.getBoardInfo(boardId);
        return ResponseEntity.status(HttpStatus.OK).body(boardInfo);
    }

    @Operation(summary = "게시판 이름 조회", description = "게시판 ID로 게시판 이름을 조회합니다.")
    @GetMapping("/name")
    public ResponseEntity<String> getBoardNameById(
            @RequestParam Long boardId) {
        String boardName = boardService.getBoardNameById(boardId);
        return ResponseEntity.status(HttpStatus.OK).body(boardName);
    }
}
