package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.BoardSaveDto;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardRedisService {

    private static final String BOARD_RANK_KEY = "board_rank";
    private static final String BOARD_RANK_VALID_KEY = "board_rank_valid";
    private static final String BOARD_REALTIME_RANK_KEY = "board_realtime_rank";
    private static final long KEY_LIVE_TIME = 301L;
    private static final int KEY_EXPIRE = 0;

    private final RedisTemplate<String, String> redisTemplate;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getName();
        long keyLiveTime = KEY_LIVE_TIME;

        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
            keyLiveTime += currentExpire;
        }

        redisTemplate.opsForValue().set(key, "실시간 게시글");
        redisTemplate.expire(key, keyLiveTime, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(BOARD_RANK_KEY, key, score);
        redisTemplate.opsForZSet().add(BOARD_REALTIME_RANK_KEY, key, score);
    }

    public void setRankValidListTime() {
        String validTime = Long.toString(LocalTime.now().plusSeconds(KEY_LIVE_TIME).toSecondOfDay());
        redisTemplate.opsForValue().set(BOARD_RANK_VALID_KEY, validTime);
    }

    public void cleanUpExpiredKeys() {
        redisTemplate.opsForZSet().removeRange(BOARD_REALTIME_RANK_KEY, 0, -1);

        Set<String> allRankKey = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
        if(allRankKey == null || allRankKey.isEmpty()) {
            return;
        }

        for(String key : allRankKey) {
            if(!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.opsForZSet().remove(BOARD_RANK_KEY, key);
            }
        }
    }

    public boolean isRealTimeBoard(BoardSaveDto boardSaveDto) {
        String boardName = boardSaveDto.getName();
        return redisTemplate.opsForValue().get(boardName) != null;
    }
}
