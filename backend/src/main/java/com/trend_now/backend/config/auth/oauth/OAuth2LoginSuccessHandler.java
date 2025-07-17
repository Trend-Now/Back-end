package com.trend_now.backend.config.auth.oauth;

import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.domain.Members;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        log.info("OAuth2 Login 성공, JWT 생성 시작");
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        // CustomOAuth2UserService에서 저장한 Member 객체 가져오기
        Members member = customUserDetails.getMembers();

        // JWT 토큰 생성
        String jwtToken = jwtTokenProvider.createToken(member.getId());

        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(HttpServletResponse.SC_OK); // 200 OK

        response.getWriter().write("{\"jwt\": \"" + jwtToken + "\"}");
    }
}
