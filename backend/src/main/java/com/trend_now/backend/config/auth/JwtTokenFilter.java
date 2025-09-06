package com.trend_now.backend.config.auth;

import com.trend_now.backend.common.CookieUtil;
import com.trend_now.backend.exception.CustomException.InvalidTokenException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;

    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String JWT_PREFIX = "Bearer ";
    private static final String INVALID_TOKEN = "유효하지 않은 토큰입니다.";

    @Value("${jwt.access-token.secret}")
    private String secretKey;

    /**
     * 특정 path 요청은 filter 제외하도록 명시
     * todo. 필요 부분은 아래에 추가
     */
    private final List<String> excludedPaths = Arrays.asList(
            "/api/v1/member/login",
            "/api/v1/member/access-token",
            "/api/v1/member/test-jwt"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();

        // 제외할 경로인지 확인
        return excludedPaths.stream()
                .anyMatch(requestURI::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // HttpServletRequest 객체 Cookie에서 토큰 값 추출
        String accessToken = CookieUtil.getCookie(request, ACCESS_TOKEN_KEY)
                .map(Cookie::getValue)
                .orElse(null);
        log.info("[JwtTokenFilter.doFilter] Cookie에서 JWT 토큰 추출: {}", accessToken);

        try {
            if (accessToken != null) {
                // Access Token 검증 및 Claims 객체 추출
                Claims claims = validateAccessToken(accessToken);
                if (claims == null) {
                    throw new InvalidTokenException(INVALID_TOKEN);
                }

                /**
                 *  인증 객체 범위
                 *  - SecurityContextHolder 객체는 SecurityContext 객체를 감싸고 있다.
                 *  - SecurityContext 객체는 Authentication 객체를 감싸고 있다.
                 *  - Authentication 객체는 사용자 인증 정보를 가지고 있다.
                 *
                 *  Spring 전역에서 SecurityContextHolder 객체를 사용할 수 있기에, 사용자 인증 정보를 바로 확인 가능하다.
                 *  - String 사용자 정보 = SecurityContextHolder.getContext().getAuthentication().getName();
                 */
                // Authentication 객체 생성
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());
                log.info("[JwtTokenFilter.doFilterInternal] 생성된 UserDetails 객체 데이터 : {} ", userDetails.toString());

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, accessToken, userDetails.getAuthorities());

                log.info("[JwtTokenFilter.doFilterInternal] 생성된 Authentication 객체 데이터 : {} ", authentication);
                // SecurityContextHolder 객체에 사용자 정보 객체 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        }

        // JWT 검증에서 예외가 발생하면 doFilter() 메서드를 통해 필터 체인에 접근하지 않고 사용자에게 에러를 반환
        catch (Exception e) {
            log.error("[JwtTokenFilter.doFilter] : Access Token이 올바르지 않습니다.");
            e.printStackTrace();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("invalid token");
        }
    }

    /**
     *  JWT 검증 및 claims(payload) 추출
     *  - 입력된 JWT 토큰의 (헤더 + 페이로드) 부분을 secretKey를 가지고 헤더에 명시된 암호화 알고리즘을 진행
     *  - 암호화 알고리즘을 통해 나온 문자열인 서명을 입력받은 토큰의 서명 부분과 비교
     *  - 일치하면 true, 불일치면 false 반환
     *  - getBody()를 통해 페이로드 부분(Claims) 추출
     */
    public Claims validateAccessToken(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (Exception e) {
            log.error("[JwtTokenFilter.validateAccessToken] : 유효하지 않은 Access Token 입니다. {}", e.getMessage());
            return null;
        }
    }
}