package com.trend_now.backend.config.auth;

import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JwtTokenFilter extends GenericFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String JWT_PREFIX = "Bearer ";
    private final MemberRepository memberRepository;

    private static final String NOT_EXIST_MEMBER = "존재하지 않는 회원입니다.";

    @Value("${jwt.secret}")
    private String secretKey;

    public JwtTokenFilter(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // HTTP 내부 데이터를 조금 더 편하게 쓸려고 다운캐스팅 진행
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        // HttpServletRequest 객체 Header에서 토큰 값 추출
        String token = httpServletRequest.getHeader(AUTHORIZATION);

        try {
            if (token != null) {
                if (!token.substring(0, 7).equals(JWT_PREFIX)) {
                    log.error("[JwtTokenFilter.doFilter] : Bearer 형식이 아닙니다.");
                    throw new AuthenticationException("Bearer 형식이 아닙니다.");
                }

                String jwtToken = token.substring(7);

                /**
                 *  JWT 검증 및 claims(payload) 추출
                 *  - 입력된 JWT 토큰의 (헤더 + 페이로드) 부분을 secretKey를 가지고 헤더에 명시된 암호화 알고리즘을 진행
                 *  - 암호화 알고리즘을 통해 나온 문자열인 서명을 입력받은 토큰의 서명 부분과 비교
                 *  - 일치하면 true, 불일치면 false 반환
                 *  - getBody()를 통해 페이로드 부분(Claims) 추출
                 */
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();


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
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));
                UserDetails userDetails = new User(claims.getSubject(), "", authorities);   // password는 의미가 없어 빈 문자열 입력

                Members members = memberRepository.findByEmail(claims.getSubject())
                        .orElseThrow(() -> new BadCredentialsException(NOT_EXIST_MEMBER));
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(members, jwtToken, userDetails.getAuthorities());

                // SecurityContextHolder 객체에 사용자 정보 객체 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            chain.doFilter(request, response);
        }

        // JWT 검증에서 예외가 발생하면 doFilter() 메서드를 통해 필터 체인에 접근하지 않고 사용자에게 에러를 반환
        catch (Exception e) {
            log.error("[JwtTokenFilter.doFilter] : JWT이 올바르지 않습니다.");
            e.printStackTrace();
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write("invalid token");
        }

    }
}
