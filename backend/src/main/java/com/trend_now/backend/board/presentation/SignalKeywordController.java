package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.SignalKeywordService;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.TimeSyncDto;
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
public class SignalKeywordController {

    private static final String SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE = "실시간 검색어 컨트롤러가 호출되었습니다.";
    private static final String TIME_SYNC_SUCCESS_MESSAGE = "시간 동기화 컨트롤러가 호출되었습니다.";

    private final SignalKeywordService signalKeywordService;
    private final BoardRedisService boardRedisService;

    @GetMapping("/news/realtime")
    public Mono<SignalKeywordDto> getRealTimeNews() {
        log.info(SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE);
        return signalKeywordService.fetchRealTimeKeyword();
    }
    
    @GetMapping("/timeSync")
    public ResponseEntity<TimeSyncDto> getServerTimeSync() {
        log.info(TIME_SYNC_SUCCESS_MESSAGE);
        String boardRankValidTime = boardRedisService.getBoardRankValidTime();
        TimeSyncDto timeSyncDto = new TimeSyncDto(boardRankValidTime);
        return ResponseEntity.status(HttpStatus.OK).body(timeSyncDto);
    }
}
