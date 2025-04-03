package com.trend_now.backend.config;

import com.trend_now.backend.board.application.RedisPublisher;
import com.trend_now.backend.board.dto.RealTimeBoardKeyExpiredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

    private final RedisPublisher redisPublisher;

    public RedisKeyExpiredListener(
            RedisMessageListenerContainer listenerContainer,
            RedisPublisher redisPublisher) {
        super(listenerContainer);
        this.redisPublisher = redisPublisher;
    }

    /**
     * redis의 key가 expired 이벤트가 발생하였을 때 실행되는 메서드로
     * redis 채널(realtime-board-events)에 실시간 게시판(messageToStr)의 만료 이벤트를 발행한다
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageToStr = message.toString();
        log.info("RedisKeyExpiredListener에서 수신된 데이터 : {}", messageToStr);
        redisPublisher.publishRealTimeBoardExpiredEvent(
                RealTimeBoardKeyExpiredEvent.of(messageToStr));
    }
}
