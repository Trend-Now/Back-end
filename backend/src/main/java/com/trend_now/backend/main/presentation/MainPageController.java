package com.trend_now.backend.main.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.main.dto.MainPageDto;
import com.trend_now.backend.member.domain.Members;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "메인 페이지", description = "메인 페이지 API")
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class MainPageController {

    private static final String NOT_LOGIN_MEMBER_NAME = "Guest";
    private static final int FIRST_PAGE = 0;
    private static final int FIRST_PAGE_SIZE = 10;

    private final BoardRedisService boardRedisService;

    @GetMapping("/loadMain")
    @Operation(summary = "로그인 사용자 메인 페이지 로드", description = "로그인한 사용자가 서비스에 처음 접근하였을 때 호출하는 API입니다.")
    public ResponseEntity<MainPageDto> loadMainPage(
            @AuthenticationPrincipal(expression = "members") Members members) {

        log.info("로그인한 사용자가 메인 페이지를 로드하였습니다.");

        BoardPagingRequestDto boardPagingRequestDto = new BoardPagingRequestDto(FIRST_PAGE,
                FIRST_PAGE_SIZE);
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                boardPagingRequestDto);
        String boardRankValidTime = boardRedisService.getBoardRankValidTime();

        MainPageDto mainPageDto = MainPageDto.of(members.getName(), boardRankValidTime,
                allRealTimeBoardPaging);

        return ResponseEntity.status(HttpStatus.OK).body(mainPageDto);
    }

    @GetMapping("/loadMainNotLogin")
    @Operation(summary = "로그인하지 않은 사용자 메인 페이지 로드", description = "로그인하지 않은 사용자가 서비스에 처음 접근하였을 때 호출하는 API입니다.")
    public ResponseEntity<MainPageDto> loadMainPageNotLogin() {

        log.info("로그인하지 않은 사용자가 메인 페이지를 로드하였습니다.");

        BoardPagingRequestDto boardPagingRequestDto = new BoardPagingRequestDto(FIRST_PAGE,
                FIRST_PAGE_SIZE);
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                boardPagingRequestDto);
        String boardRankValidTime = boardRedisService.getBoardRankValidTime();

        MainPageDto mainPageDto = MainPageDto.of(NOT_LOGIN_MEMBER_NAME, boardRankValidTime,
                allRealTimeBoardPaging);

        return ResponseEntity.status(HttpStatus.OK).body(mainPageDto);
    }
}
