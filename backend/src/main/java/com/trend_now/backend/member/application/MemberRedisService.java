package com.trend_now.backend.member.application;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public static final String REFRESH_TOKEN_PREFIX = "RefreshToken_";
    private static final String NOT_EXIST_REFRESH_TOKEN_IN_REDIS = "Redis에 입력된 Member Id의 key가 없습니다.";

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
     * - 일치하는 것이 없어 NPE 발생하면 "NOT_EXIST_MATCHED_REFRESH_TOKEN_IN_REDIS" 예외 발생
     */
    public boolean isMatchedRefreshTokenInRedis(Long memberIdInAccessToken, String refreshToken) {
        String refreshTokenInRedis = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberIdInAccessToken);
        log.info("[MemberRedisService.isMatchedRefreshTokenInRedis] Redis 내부에 저장된 Member Id {} 의 Refresh Token = {}",
                        memberIdInAccessToken, refreshTokenInRedis);

        if(Objects.isNull(refreshTokenInRedis)) {
            throw new NotFoundException(NOT_EXIST_REFRESH_TOKEN_IN_REDIS);
        }

        return refreshTokenInRedis.equals(refreshToken);
    }
}
