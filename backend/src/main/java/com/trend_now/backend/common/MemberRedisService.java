package com.trend_now.backend.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberRedisService {

    private final RedisTemplate<String, String> redisTemplate;


    /**
     * Redis에 Refresh Token 저장
     * - key가 Refresh Token, value가 Member Id가 된다.
     * - Access Token 재발급 시, key로서 Refresh Token을 받기 위함
     */
    public void saveRefreshToken(String refreshToken, Long memberId, int refreshTokenExpiration) {
        redisTemplate.opsForValue().set(
                refreshToken,
                String.valueOf(memberId),
                Duration.ofMinutes(refreshTokenExpiration)
        );

        log.info("[RedisUtil.saveRefreshToken] Refresh Token 저장 완료, token={}, memberId={}, 만료={}분",
                refreshToken, memberId, refreshTokenExpiration);
    }

    /**
     * Redis에 Refresh Token을 이용해 Member Id 조회
     */
    public String findMemberIdByRefreshToken(String refreshToken) {
        String memberId = redisTemplate.opsForValue().get(refreshToken);

        log.info("[RedisUtil.findMemberIdByRefreshToken] memberId={}", memberId);
        return memberId;
    }
}
