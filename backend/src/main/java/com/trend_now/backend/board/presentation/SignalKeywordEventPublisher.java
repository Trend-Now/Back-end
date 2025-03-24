package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalKeywordEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishEvent(SignalKeywordEventDto event) {
        log.info("이벤트가 발행되었습니다.");
        eventPublisher.publishEvent(event);
    }
}
