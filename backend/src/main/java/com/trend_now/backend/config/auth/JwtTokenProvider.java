package com.trend_now.backend.config.auth;

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

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration}") int expiration) {
        this.secretKey = secretKey;
        this.expiration = expiration;

        /**
         *  JWT 서명에 사용되는 secret key 생성
         *  - application.yml에서 가져온 인코딩된 secretKey를 디코딩한 후,
         *  - HS512 알고리즘을 통해 암호화
         */
        this.SECRET_KEY = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKey),
                SignatureAlgorithm.HS512.getJcaName());
    }

    public String createToken(String email, String role) {
        /**
         *  Claims은 JWT의 페이로드의 데이터 단위
         *  - setSubject() 메서드를 통해 JWT를 인증할 식별자를 원하는 데이터로 지정 가능
         */
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);   // private Claim으로 협의하에 사용
        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(SECRET_KEY)
                .compact();

        return token;
    }
}
