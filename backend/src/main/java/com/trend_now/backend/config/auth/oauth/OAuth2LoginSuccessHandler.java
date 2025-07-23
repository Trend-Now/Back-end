package com.trend_now.backend.config.auth.oauth;

import com.trend_now.backend.common.CookieUtil;
import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.domain.Members;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("소셜 로그인 성공, JWT 생성 시작");
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        // CustomOAuth2UserService에서 저장한 Member 객체 가져오기
        Members member = customUserDetails.getMembers();

        // JWT 토큰 생성
        String jwtToken = jwtTokenProvider.createToken(member.getId());

        // 쿠키에서 가져온 redirect URL을 가져온다
        String redirectUrl = CookieUtil.getCookie(request, CustomAuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
            .map(Cookie::getValue)
            .orElse("/");

        // 리다이렉트 할 URL에 JWT 토큰을 쿼리 파라미터로 추가
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
            .queryParam("jwt", jwtToken)
            .build()
            .toUriString();

        response.sendRedirect(targetUrl);
    }
}
