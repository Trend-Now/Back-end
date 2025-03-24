package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.SignalKeywordService;
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

    private final SignalKeywordService signalKeywordService;

    @Async
    @EventListener
    public void handleEvent(SignalKeywordEventDto event) {
        log.info("이벤트가 핸들러에서 이벤트 발행을 감지했습니다.");
        signalKeywordService.sendKeywordList(event);
    }
}
