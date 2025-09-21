package com.trend_now.backend.config;

import com.trend_now.backend.config.auth.oauth.CustomAuthenticationEntryPoint;
import com.trend_now.backend.config.auth.JwtTokenFilter;
import com.trend_now.backend.config.auth.oauth.CustomAuthorizationRequestRepository;
import com.trend_now.backend.config.auth.oauth.OAuth2LoginFailureHandler;
import com.trend_now.backend.config.auth.oauth.OAuth2LoginSuccessHandler;
import com.trend_now.backend.config.auth.oauth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${chrome.extension.id}")
    private static String CHROME_EXTENSION_ID;

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

            // 특정 url 패턴에 대해서는 security filter에서 예외가 발생해도 정상 실행(Authentication 객체를 안만들겠다는 의미)
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET,
                    // 게시판과 게시글 조회 허용
                    "/api/v1/boards/**",
                    "/api/v1/search/**",
                    // ALB 헬스 체크를 위한 엔드포인트 허용
                    "/health"
                ).permitAll()
                .requestMatchers(
                    "/api/v1/member/login/**", "/swagger-ui/**", "/v3/api-docs/**",
                    "/api/v1/news/realtime", "/api/v1/timeSync", "/api/v1/subscribe",
                    "/api/v1/unsubscribe", "/sse-test", "/api/v1/member/test-jwt",
                    "/oauth2/authorization/**", "/login/oauth2/code/**", // OAuth2 로그인 관련 URL 허용
                    "/api/v1/member/access-token"   // Access Token 재발급
                ).permitAll()
                .anyRequest().authenticated())
            // 인증되지 않은 사용자가 접근할 경우, CustomAuthenticationEntryPoint를 통해 예외 처리
            .exceptionHandling(e -> e.authenticationEntryPoint(customAuthenticationEntryPoint))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler) // 로그인 성공 핸들러 등록
                .failureHandler(oAuth2LoginFailureHandler) // 로그인 실패 핸들러 등록
                // HttpSessionOAuth2AuthorizationRequestRepository 대신 CustomAuthorizationRequestRepository을 사용하도록 등록
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestRepository(authorizationRequestRepository())
                )
            )

            /**
             *  UsernamePasswordAuthenticationFilter는 Spring Security에서 제공하는 로그인 폼을 의미한다.
             *  해당 UsernamePasswordAuthenticationFilter 필터 전에, JwtTokenFilter를 사용하여 Authentication 객체를 생성
             *  UsernamePasswordAuthenticationFilter를 무용지물로 만드는 것
             */
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new CustomAuthorizationRequestRepository();
    }

    /**
     * CORS 허용하는 도메인들을 명시한다. 허용하지 않는 도메인의 요청은 에러를 반환시켜 프론트엔드와 통신이 불가능해진다. 해당 메서드에 개발용 프론트엔드, 배포용
     * 프론트엔드 도메인을 추가한다.
     * <p>
     *  todo. 추후에 프론트엔드 도메인 추가
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            Arrays.asList("http://localhost:3000", "http://localhost:63342", "http://local.trendnow.me:3000",
                // 개발 프론트엔드 도메인 허용
                "https://www.trendnow.me",
                "https://front-end-git-bugfix-login-trendnow.vercel.app",
                "https://front-end-git-dev-trendnow.vercel.app",
                // 크롬 익스텐션 도메인 허용
                "chrome-extension://" + CHROME_EXTENSION_ID));
        configuration.setAllowedMethods(Arrays.asList("*"));    // 모든 HTTP 메서드 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));    // 모든 헤더 값 허용
        configuration.setAllowCredentials(true);    // 자격 증명을 허용(Authorization 헤더를 허용 목적)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 url 패턴에 대해 CORS 허용 설정
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
