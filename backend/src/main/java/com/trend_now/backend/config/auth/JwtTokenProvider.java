package com.trend_now.backend.config.auth;

import com.trend_now.backend.common.AesUtil;
import com.trend_now.backend.member.application.MemberRedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final String secretKey;     // 인코딩된 secret key
    private final int expiration;       // application.yml 에서 가져온 분 단위의 만료 시간
    private Key SECRET_KEY;     // 서명에 사용되는 secret key
    private final int refreshTokenExpiration;       // Refresh Token 만료 기간

    private final MemberRedisService memberRedisService;
    private final AesUtil aesUtil;

    public JwtTokenProvider(@Value("${jwt.access-token.secret}") String secretKey, @Value("${jwt.access-token.expiration}") int expiration,
                            MemberRedisService memberRedisService, @Value("${jwt.refresh-token.expiration}") int refreshTokenExpiration, AesUtil aesUtil) {
        this.secretKey = secretKey;
        this.expiration = expiration;
        this.memberRedisService = memberRedisService;
        this.refreshTokenExpiration = refreshTokenExpiration;

        /**
         *  JWT 서명에 사용되는 secret key 생성
         *  - application.yml에서 가져온 인코딩된 secretKey를 디코딩한 후,
         *  - HS512 알고리즘을 통해 암호화
         */
        this.SECRET_KEY = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKey),
                SignatureAlgorithm.HS512.getJcaName());
        this.aesUtil = aesUtil;
    }

    /**
     *  Access Token 토큰을 생성할 때, 사용자 식별이 가능한 PK 값을 이용
     *  - setSubject() 메서드를 통해 memberId를 JWT의 sub Claim에 저장
     */
    public String createAccessToken(Long memberId) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(memberId));
        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(SECRET_KEY)
                .compact();

        log.info("[JwtTokenProvider.createAccessToken] 생성된 JWT = {}", token);
        return token;
    }

    /**
     *  Refresh Token 생성 메서드
     *  - Refresh Token은 AES 암호화를 통해 생성
     *  - Redis에 아래 형식의 key : value 를 저장한다.
     *      - [유저 식별자] : [Refresh Token 문자열]
     *      - Redis 데이터 만료 시간은 yml에 지정
     *  - Redis에 동일 key(유저 식별자)가 존재하면 Redis 자료구조에 의해 value(Refresh Token)과 만료 시간이 새 것으로 대체
     */
    public String createRefreshToken(Long memberId) {
        String refreshToken =aesUtil.encrypt(memberId.toString());

        log.info("[JwtTokenProvider.createRefreshToken] 생성된 Refresh Token = {}", refreshToken);

        // Redis에 저장
        memberRedisService.saveRefreshToken(memberId, refreshToken, refreshTokenExpiration);

        return refreshToken;
    }
}
