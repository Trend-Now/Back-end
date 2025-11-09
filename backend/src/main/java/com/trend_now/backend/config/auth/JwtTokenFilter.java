package com.trend_now.backend.config.auth;

import com.trend_now.backend.common.CookieUtil;
import com.trend_now.backend.exception.customException.ExpiredTokenException;
import com.trend_now.backend.exception.customException.InvalidTokenException;
import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.member.application.MemberRedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final MemberRedisService memberRedisService;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String INVALID_TOKEN_RETURN_MESSAGE = "Invalid Access Token";
    private static final String EXPIRED_TOKEN_RETURN_MESSAGE = "Expired Access Token";
    private static final int ONE_YEAR = 365*24*60*60;   // 1년을 초로 변환

    @Value("${jwt.access-token.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private int accessTokenExpiration;
    
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // HttpServletRequest 객체 Cookie에서 토큰 값 추출
        String accessToken = CookieUtil.getCookie(request, ACCESS_TOKEN_KEY)
                .map(Cookie::getValue)
                .orElse(null);

        String refreshToken = CookieUtil.getCookie(request, REFRESH_TOKEN_KEY)
                .map(Cookie::getValue)
                .orElse(null);

        try {
            if (accessToken != null) {
                // Access Token 검증 및 Claims 객체 추출
                Claims claims = validateAccessToken(accessToken);
                setAuthentication(claims, accessToken);
            }
        }

        /**
         * 만료된 Access Token의 경우
         * - 만료된 Access Token에서 memberId 추출
         * - memberId를 통해 Refresh Token 검증 후, Access Token 재발급 또는 다음 필터 진행
         */
        catch (ExpiredTokenException e) {
            log.info("[JwtTokenFilter.doFilter] : 만료된 Access Token으로 API 요청이 들어왔습니다.");
            Long memberIdInExpiredAccessToken = extractMemberId(accessToken);
            boolean isAccessTokenReissuable = false;

            try {
                isAccessTokenReissuable =
                        memberRedisService.isMatchedRefreshTokenInRedis(memberIdInExpiredAccessToken, refreshToken);
            } catch (NotFoundException nfe) {
                log.info("[JwtTokenFilter.validateAccessToken] Redis에 매칭되는 Refresh Token 존재하지 않습니다.");
            }

            if (isAccessTokenReissuable) {
                String reissuancedAccessToken = jwtTokenProvider.createAccessToken(memberIdInExpiredAccessToken);
                CookieUtil.addCookie(request, response, ACCESS_TOKEN_KEY, reissuancedAccessToken, ONE_YEAR);

                // Refresh Token이 검증된 경우에만 Authentication 객체를 지정
                Claims expiredClaims = extractClaimsFromExpiredToken(accessToken);
                setAuthentication(expiredClaims, accessToken);

            }
        }

        /**
         * Invalid Access Token 또는 다른 JWT 예외의 경우
         * - Authentication 객체를 AnonymousUser로 지정
         */
        catch (InvalidTokenException e) {
            log.error("[JwtTokenFilter.doFilter] : Access Token이 올바르지 않습니다.");
        }

        catch (Exception e) {
            log.error("[JwtTokenFilter.doFilter] : 예외 발생 - {}", e.getMessage());
        }

        /**
         * JwtTokenFilter 측에서는 예외가 발생 시, Authentication 객체만 AnonymousUser로 지정하고 다음 필터로 진행
         */
        finally {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 인증 객체 지정 메서드
     * - SecurityContextHolder 객체는 SecurityContext 객체를, SecurityContext 객체는 Authentication 객체를 포함
     * - Authentication 객체는 사용자 인증 정보를 보유
     * - Spring 전역에서 SecurityContextHolder 객체를 사용할 수 있기에, 사용자 인증 정보를 바로 확인 가능하다.
     */
    private void setAuthentication(Claims claims, String accessToken) {
        // Authentication 객체 생성
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());
        log.info("[JwtTokenFilter.doFilterInternal] 생성된 UserDetails 객체 데이터 : {} ", userDetails.toString());

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, accessToken, userDetails.getAuthorities());

        log.info("[JwtTokenFilter.doFilterInternal] 생성된 Authentication 객체 데이터 : {} ", authentication);
        // SecurityContextHolder 객체에 사용자 정보 객체 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 만료된 Access Token에서 Claim 추출 메서드
     */
    private Claims extractClaimsFromExpiredToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     *  JWT 검증 및 claims(payload) 추출
     *  - 입력된 JWT 토큰의 (헤더 + 페이로드) 부분을 secretKey를 가지고 헤더에 명시된 암호화 알고리즘을 진행
     *  - 암호화 알고리즘을 통해 나온 문자열인 서명을 입력받은 토큰의 서명 부분과 비교
     *  - 일치하면 true, 불일치면 false 반환
     *  - getBody()를 통해 페이로드 부분(Claims) 추출
     *
     *  - parse를 진행하면서 만료 또는 서명 등은 라이브러리 자체 검증이 진행
     */
    public Claims validateAccessToken(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException(EXPIRED_TOKEN_RETURN_MESSAGE);
        } catch (JwtException e) {
            throw new InvalidTokenException(INVALID_TOKEN_RETURN_MESSAGE);
        }
    }

    /**
     * JWT에서 Member ID 추출 메서드
     * - 만료된 JWT에서도 Member ID는 추출 가능하도록 처리(ex. Access Token 재발급)
     */
    public Long extractMemberId(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(accessToken)
                    .getBody();

            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return Long.valueOf(claims.getSubject());
        }
    }
}