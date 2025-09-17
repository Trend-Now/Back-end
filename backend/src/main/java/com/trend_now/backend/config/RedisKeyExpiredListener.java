package com.trend_now.backend.config;

import static com.trend_now.backend.board.application.BoardRedisService.BOARD_RANK_KEY;
import static com.trend_now.backend.member.application.MemberRedisService.REFRESH_TOKEN_PREFIX;
import static com.trend_now.backend.post.application.PostsService.POST_COOLDOWN_PREFIX;

import com.trend_now.backend.board.application.RedisPublisher;
import com.trend_now.backend.board.dto.RealTimeBoardKeyExpiredEvent;
import com.trend_now.backend.comment.application.CommentsService;
import com.trend_now.backend.post.application.PostsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisPublisher redisPublisher;
    private final RedisMessageListenerContainer listenerContainer;
    private final PostsService postsService;
    private final CommentsService commentsService;

    private static final String BOARD_KEY_DELIMITER = ":";

    public RedisKeyExpiredListener(
            RedisMessageListenerContainer listenerContainer,
            RedisTemplate<String, String> redisTemplate,
            RedisPublisher redisPublisher, PostsService postsService, CommentsService commentsService) {
        super(listenerContainer);
        this.redisPublisher = redisPublisher;
        this.listenerContainer = listenerContainer;
        this.postsService = postsService;
        this.commentsService = commentsService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void init() {
        // CONFIG 명령어를 사용하는 super.init() 호출을 피하고 직접 등록 메서드 호출
        super.doRegister(this.listenerContainer);
    }

    /**
     * redis의 key가 expired 이벤트가 발생하였을 때 실행되는 메서드로
     * redis 채널(realtime-board-events)에 실시간 게시판(messageToStr)의 만료 이벤트를 발행한다
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageToStr = message.toString();
        // 게시판이 만료 되면 게시판에 속한 게시글과 댓글의 modifiable를 false로 변경
        String[] key = messageToStr.split(BOARD_KEY_DELIMITER);
        // 만약 게시판 쿨다운이거나 Refresh Token의 TTL이라면 무시
        if (key[0].equals(POST_COOLDOWN_PREFIX) || key[0].equals(REFRESH_TOKEN_PREFIX)) {
            return;
        }
        postsService.updateModifiable(Long.parseLong(key[1]));
        commentsService.updateModifiable(Long.parseLong(key[1]));

        redisTemplate.opsForZSet().remove(BOARD_RANK_KEY, messageToStr);

        log.info("RedisKeyExpiredListener에서 수신된 데이터 : {}", messageToStr);
        redisPublisher.publishRealTimeBoardExpiredEvent(
                RealTimeBoardKeyExpiredEvent.of(messageToStr));
    }
}
