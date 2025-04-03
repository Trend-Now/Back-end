/*
 * 클래스 설명 : SSE 이벤트가 publisher에게 도달했을 때, 지정된 토픽(채널)으로 이벤트를 발행하는 클래스 (Redis Pub/Sub 중 Pub에 해당)
 * 메소드 설명
 * - publish() : 발행된 event를 지정된 토픽(채널, 현재는 sse-events : RedisConfig 참고)으로 전송하는 메서드
 */
package com.trend_now.backend.board.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.dto.RealTimeBoardKeyExpiredEvent;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic signalKeywordEventTopic;
    private final ChannelTopic realTimeBoardEventTopic;

    /** publish를 호출하면, topic을 구독하는 모든 구독자에게 message가 발행 (pub) */

    /**
     * 실시간 검색어 순위 publish
     */
    public void publishSignalKeywordEvent(SignalKeywordEventDto event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            log.info("RedisPublisher(실시간 검색어 갱신)가 채널: {}에 이벤트: {}를 발행했습니다.",
                    signalKeywordEventTopic.getTopic(), event);
            redisTemplate.convertAndSend(signalKeywordEventTopic.getTopic(), message);
        } catch (JsonProcessingException e) {
            log.info("RedisPublisher(실시간 검색어 갱신)에서 이벤트 변환 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 실시간 게시판 만료 publish
     */
    public void publishRealTimeBoardExpiredEvent(RealTimeBoardKeyExpiredEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            log.info("RedisPublisher(실시간 게시판 만료)가 채널: {}에 이벤트: {}를 발행했습니다.",
                    realTimeBoardEventTopic.getTopic(), event);
            redisTemplate.convertAndSend(realTimeBoardEventTopic.getTopic(), message);
        } catch (JsonProcessingException e) {
            log.info("RedisPublisher(실시간 게시판 만료)에서 이벤트 변환 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
