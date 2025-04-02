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

    private static final String SUBSCRIBER_LISTENER_METHOD_NAME = "sendKeywordListBySubscriber";
    private static final String SSE_EVENT_TOPIC_NAME = "sse-events";

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public ChannelTopic sseEventTopic() {
        return new ChannelTopic(SSE_EVENT_TOPIC_NAME);
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
            MessageListenerAdapter sseEventListenerAdapter,
            ChannelTopic topic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory());
        container.addMessageListener(sseEventListenerAdapter, topic);
        return container;
    }

    /**
     * Redis에 발행되는 실제 이벤트(실시간 검색어 순위)를 처리하는 subscriber 설정 추가
     */
    @Bean
    public MessageListenerAdapter sseEventListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, SUBSCRIBER_LISTENER_METHOD_NAME);
    }
}
