/*
 * 클래스 설명 : SSE 이벤트가 단일 서버일 때 발행하는 데 쓰이는 클래스 (Redis Pub/Sub에서는 사용하지 않음)
 * 메소드 설명
 * - publishEvent() : SignalKeywordEventDto에 실시간 검색어 순위가 저장되어 있고, SSE 이벤트를 발행하는 메서드
 */
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
