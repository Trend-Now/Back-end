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
     * Redis에 key(Member Id)에 value가 Refresh Token가 일치하는 확인 메서드
     * - 일치하는 것이 없어 NPE 발생하면 호출 메서드에서 "NOT_EXIST_MATCHED_REFRESH_TOKEN_IN_REDIS" 예외 발생
     */
    public void isMatchedRefreshTokenInRedis(Long memberIdInAccessToken, String RefreshToken) {
        redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberIdInAccessToken).equals(RefreshToken);
    }
}
