package com.trend_now.backend.config;

import com.trend_now.backend.config.auth.JwtTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;

    @Bean
    public PasswordEncoder makePassword() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain myFilter(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 프론트엔드가 별도 존재하므로 CSRF 공격 보안에 대해 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // Basic 인증은 사용자 이름과 비밀번호를 Base64 인코딩하여 인증값으로 사용한다. 여기서는 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 토큰 방식을 이용할 것이니 sessionManagement 비활성화
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 특정 url 패턴에 대해서는 security filter에서 제외(Authentication 객체를 안만들겠다는 의미)
                // todo. 비회원도 사용 가능한 API의 uri 패턴 추가
                .authorizeHttpRequests(a -> a.requestMatchers(
                                "/api/v1/member/login/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/boards/list", "/api/v1/news/realtime",
                                "/api/v1/timeSync", "/api/v1/subscribe", "/api/v1/unsubscribe", "/sse-test"
                        , "/api/v1/member/test-jwt")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/comments/*")
                        .permitAll()
                        .anyRequest().authenticated())

                /**
                 *  UsernamePasswordAuthenticationFilter는 Spring Security에서 제공하는 로그인 폼을 의미한다.
                 *  해당 UsernamePasswordAuthenticationFilter 필터 전에, JwtTokenFilter를 사용하여 Authentication 객체를 생성
                 *  UsernamePasswordAuthenticationFilter를 무용지물로 만드는 것
                 */
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     *  CORS 허용하는 도메인들을 명시한다.
     *  허용하지 않는 도메인의 요청은 에러를 반환시켜 프론트엔드와 통신이 불가능해진다.
     *  해당 메서드에 개발용 프론트엔드, 배포용 프론트엔드 도메인을 추가한다.
     *
     *  todo. 추후에 프론트엔드 도메인 추가
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:63342"));    // 개발 프론트엔드 도메인 허용
        configuration.setAllowedMethods(Arrays.asList("*"));    // 모든 HTTP 메서드 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));    // 모든 헤더 값 허용
        configuration.setAllowCredentials(true);    // 자격 증명을 허용(Authorization 헤더를 허용 목적)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 url 패턴에 대해 CORS 허용 설정
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
