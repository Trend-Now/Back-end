/*
 * 클래스 설명 : SSE 이벤트가 발행되었는지 감시하는 클래스
 * 메소드 설명
 * - handleEvent() : SSE 이벤트가 발행되었는지 확인하고 비동기로 SSE 알림(실시간 검색어 순위를 클라이언트에게 전송하라는 알림)을
 *                   클라이언트에게 보내는 클래스
 */
package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.SseEmitterService;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalKeywordEventHandler {

    private final SseEmitterService sseEmitterService;

    @Async
    @EventListener
    public void handleEvent(SignalKeywordEventDto event) {
        log.info("이벤트가 핸들러에서 이벤트 발행을 감지했습니다.");
        sseEmitterService.sendKeywordList(event);
    }
}
