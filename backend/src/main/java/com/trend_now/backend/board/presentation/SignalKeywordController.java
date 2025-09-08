package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.SignalKeywordService;
import com.trend_now.backend.board.dto.SseDisconnectDto;
import com.trend_now.backend.board.dto.TimeSyncDto;
import com.trend_now.backend.board.dto.Top10WithDiff;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Signal Keyword API", description = "실시간 검색어 관련 API")
public class SignalKeywordController {

    private static final String SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE = "실시간 검색어 컨트롤러가 호출되었습니다.";
    private static final String TIME_SYNC_SUCCESS_MESSAGE = "시간 동기화 컨트롤러가 호출되었습니다.";
    private static final String SUBSCRIBE_SUCCESS_MESSAGE = "사용자가 서버의 SSE 구독을 완료했습니다.";
    private static final String UNSUBSCRIBE_SUCCESS_MESSAGE = "사용자가 서버의 SSE 구독 취소를 완료했습니다.";

    private final SignalKeywordService signalKeywordService;
    private final BoardRedisService boardRedisService;

    @Operation(summary = "실시간 검색어 조회", description = "현재 실시간 검색어를 가져옵니다.")
    @GetMapping("/news/realtime")
    public List<Top10WithDiff> getRealTimeNews() {
        log.info(SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE);
        return signalKeywordService.getRealTimeKeyword();
    }

    @Operation(summary = "서버 시간 동기화", description = "서버의 최신 시간 동기화 정보를 반환합니다.")
    @GetMapping("/timeSync")
    public ResponseEntity<TimeSyncDto> getServerTimeSync() {
        log.info(TIME_SYNC_SUCCESS_MESSAGE);
        String boardRankValidTime = boardRedisService.getBoardRankValidTime();
        TimeSyncDto timeSyncDto = new TimeSyncDto(boardRankValidTime);
        return ResponseEntity.status(HttpStatus.OK).body(timeSyncDto);
    }

    @Operation(summary = "SSE 연결 시도", description = "클라이언트는 클라이언트 측에서 만들어진 랜덤 값(clientId)과 함께 SSE 연결을 시도합니다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@RequestParam String clientId) {
        return ResponseEntity.status(HttpStatus.OK).body(signalKeywordService.subscribe(clientId));
    }

    @Operation(summary = "SSE 연결 중단", description = "클라이언트는 클라이언트 측에서 만들어진 랜덤 값(clientId)과 함께 SSE 연결 중단을 시도합니다.")
    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(
            @Valid @RequestBody SseDisconnectDto sseDisconnectDto) {
        String clientId = sseDisconnectDto.getClientId();
        signalKeywordService.deleteClientId(clientId);
        return ResponseEntity.status(HttpStatus.OK).body(UNSUBSCRIBE_SUCCESS_MESSAGE);
    }
}
