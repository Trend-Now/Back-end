package com.trend_now.backend.member.application;

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

    private static final String REFRESH_TOKEN_PREFIX = "RefreshToken_";

    /**
     * Redis에 Refresh Token 저장
     * - key가 prefix + Member Id, value가 Refresh Token이 된다.
     * - ex. RefreshToken_member1 : RefreshToken1
     */
    public void saveRefreshToken(Long memberId, String refreshToken, int refreshTokenExpiration) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + memberId.toString(),
                refreshToken,
                Duration.ofMinutes(refreshTokenExpiration)
        );

        log.info("[RedisUtil.saveRefreshToken] Refresh Token 저장 완료, token={}, memberId={}, 만료={}분",
                refreshToken, memberId, refreshTokenExpiration);
    }

    /**
     * Redis에 Member Id를 이용해 Refresh Token 조회
     */
    public boolean isMemberIdInRedis(String memberId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberId) != null;
    }
}
