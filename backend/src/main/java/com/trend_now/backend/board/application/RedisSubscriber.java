/*
 * 클래스 설명 : SSE 이벤트가 subscriber에게 도달했을 때, SSE에 이벤트를 보내는 클래스 (Redis Pub/Sub 중 Sub에 해당)
 * 메소드 설명
 * - sendKeywordListBySubscriber() : SSE 이벤트가 subscriber에게 도달했을 때, SSE에 이벤트(실시간 검색어 순위)를 보내는 메소드
 */
package com.trend_now.backend.board.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.dto.RealTimeBoardKeyExpiredEvent;
import com.trend_now.backend.board.dto.RealTimeBoardTimeUpEvent;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;

    public void sendKeywordListBySubscriber(String message) {
        try {
            SignalKeywordEventDto event = objectMapper.readValue(message,
                    SignalKeywordEventDto.class);

            log.info("Redis Subscriber에서 받은 데이터 {}를 SSE를 통해 {}에게 전송한다.", event,
                    event.getClientId());

            sseEmitterService.sendKeywordList(event);
        } catch (Exception e) {
            log.error("Redis Subscriber에서 데이터 전송 중 오류가 발생했습니다.", e);
        }
    }

    public void sendRealTimeBoardExpiredBySubscriber(String message) {
        try {
            RealTimeBoardKeyExpiredEvent event = objectMapper.readValue(message,
                    RealTimeBoardKeyExpiredEvent.class);

            log.info("Redis Subscriber(실시간 게시판 만료)에서 받은 데이터 {}를 SSE를 통해 게시판 {}의 만료를 전송한다.", event,
                    event.getBoardName());

            sseEmitterService.sendRealTimeBoardExpired(event);
        } catch (Exception e) {
            log.error("Redis Subscriber(실시간 게시판 만료)에서 데이터 전송 중 오류가 발생했습니다.", e);
        }
    }

    public void sendRealTimeBoardTimeUpBySubscriber(String message) {
        try {
            RealTimeBoardTimeUpEvent event = objectMapper.readValue(message,
                    RealTimeBoardTimeUpEvent.class);

            log.info("Redis Subscriber(실시간 게시판 시간 증가)에서 받은 데이터 {}를 SSE를 통해 게시판 {}의 시간 증가({}초)를 전송한다.", event,
                    event.getBoardName(), event.getTimeUp());

            sseEmitterService.sendRealTimeBoardTimeUp(event);
        } catch (Exception e) {
            log.error("Redis Subscriber(실시간 게시판 시간 증가)에서 데이터 전송 중 오류가 발생햽.", e);
        }
    }
}
