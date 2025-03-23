package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.SignalKeywordService;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.TimeSyncDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Signal Keyword API", description = "실시간 검색어 관련 API")
public class SignalKeywordController {

    private static final String SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE = "실시간 검색어 컨트롤러가 호출되었습니다.";
    private static final String TIME_SYNC_SUCCESS_MESSAGE = "시간 동기화 컨트롤러가 호출되었습니다.";

    private final SignalKeywordService signalKeywordService;
    private final BoardRedisService boardRedisService;

    @Operation(summary = "실시간 검색어 조회", description = "현재 실시간 검색어를 가져옵니다.")
    @GetMapping("/news/realtime")
    public Mono<SignalKeywordDto> getRealTimeNews() {
        log.info(SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE);
        return signalKeywordService.fetchRealTimeKeyword();
    }

    @Operation(summary = "서버 시간 동기화", description = "서버의 최신 시간 동기화 정보를 반환합니다.")
    @GetMapping("/timeSync")
    public ResponseEntity<TimeSyncDto> getServerTimeSync() {
        log.info(TIME_SYNC_SUCCESS_MESSAGE);
        String boardRankValidTime = boardRedisService.getBoardRankValidTime();
        TimeSyncDto timeSyncDto = new TimeSyncDto(boardRankValidTime);
        return ResponseEntity.status(HttpStatus.OK).body(timeSyncDto);
    }
}
