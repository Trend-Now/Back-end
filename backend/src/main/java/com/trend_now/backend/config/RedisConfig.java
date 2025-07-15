/*
 * 클래스 설명 : Redis Pub/Sub을 위한 설정
 * 메소드 설명
 * - sseEventTopic() : Redis Pub/Sub을 위한 Topic 생성 (채널)
 * - redisMessageListener() : Redis Pub/Sub을 위한 리스너 생성
 * - sseEventListenerAdapter() : Redis Pub/Sub에 발행된 메시지를 처리하는 리스너 생성 (디자인 패턴 중 어뎁터 패턴 활용)
 */
package com.trend_now.backend.config;

import com.trend_now.backend.board.application.RedisSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

@Slf4j
@Configuration
public class RedisConfig {

    private static final String SIGNAL_KEYWORD_SUBSCRIBER_LISTENER_METHOD_NAME = "sendKeywordListBySubscriber";
    private static final String SIGNAL_KEYWORD_EVENT_TOPIC_NAME = "signal-keyword-events";
    private static final String REALTIME_BOARD_SUBSCRIBER_LISTENER_METHOD_NAME = "sendRealTimeBoardExpiredBySubscriber";
    private static final String REALTIME_BOARD_EVENT_TOPIC_NAME = "realtime-board-events";
    private static final String REALTIME_BOARD_TIMEUP_EVENT_TOPIC_NAME = "realtime-board-timeup-events";
    private static final String REALTIME_BOARD_TIMEUP_SUBSCRIBER_LISTENER_METHOD_NAME = "sendRealTimeBoardTimeUpBySubscriber";

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * redis pub/sub에 사용되는 채널로 실시간 검색어 순위 이벤트가 발행되는 채널
     */
    @Bean
    public ChannelTopic signalKeywordEventTopic() {
        return new ChannelTopic(SIGNAL_KEYWORD_EVENT_TOPIC_NAME);
    }

    /**
     * redis pub/sub에 사용되는 채널로 실시간 게시판 만료 이벤트가 발행되는 채널
     */
    @Bean
    public ChannelTopic realTimeBoardEventTopic() {
        return new ChannelTopic(REALTIME_BOARD_EVENT_TOPIC_NAME);
    }

    /**
     * redis pub/sub에 사용되는 채널로 실시간 게시판 시간 증가 이벤트가 발행되는 채널
     */
    @Bean
    public ChannelTopic realTimeBoardTimeUpEventTopic() {
        return new ChannelTopic(REALTIME_BOARD_TIMEUP_EVENT_TOPIC_NAME);
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(
                Object.class);

        RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(jackson2JsonRedisSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    /**
     * redis 에 발행(publish)된 메시지 처리를 위한 리스너 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            MessageListenerAdapter signalKeywordEventListenerAdapter,
            MessageListenerAdapter realTimeBoardEventListenerAdapter,
            MessageListenerAdapter realTimeBoardTimeUpEventListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory());
        container.addMessageListener(signalKeywordEventListenerAdapter, signalKeywordEventTopic());
        container.addMessageListener(realTimeBoardEventListenerAdapter, realTimeBoardEventTopic());
        container.addMessageListener(realTimeBoardTimeUpEventListenerAdapter,
                realTimeBoardTimeUpEventTopic());
        return container;
    }

    /**
     * Redis에 발행되는 실제 이벤트(실시간 검색어 순위)를 처리하는 subscriber 설정 추가
     */
    @Bean
    public MessageListenerAdapter signalKeywordEventListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber,
                SIGNAL_KEYWORD_SUBSCRIBER_LISTENER_METHOD_NAME);
    }

    /**
     * Redis에 발행되는 실제 이벤트(실시간 게시판 만료)를 처리하는 subscriber 설정 추가
     */
    @Bean
    public MessageListenerAdapter realTimeBoardEventListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber,
                REALTIME_BOARD_SUBSCRIBER_LISTENER_METHOD_NAME);
    }

    /**
     * Redis에 발행되는 실제 이벤트(실시간 게시판 시간 증가)를 처리하는 subscriber 설정 추가
     */
    @Bean
    public MessageListenerAdapter realTimeBoardTimeUpEventListenerAdapter(
            RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber,
                REALTIME_BOARD_TIMEUP_SUBSCRIBER_LISTENER_METHOD_NAME);
    }
}
