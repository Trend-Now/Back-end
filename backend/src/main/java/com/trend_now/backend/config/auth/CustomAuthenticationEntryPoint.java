package com.trend_now.backend.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Access Token이 없는 상태로 Access Token 필요한 페이지에 요청을 날렸을 때 실행되는 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.info("인증되지 않은 사용자에게 요청이 들어왔습니다. 요청 URI: {}", request.getRequestURI());

        // 응답 상태 코드 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 응답 컨텐츠 타입 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 응답 바디 생성
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "인증이 필요합니다. 로그인을 진행해주세요.");
        body.put("path", request.getRequestURI());

        // ObjectMapper를 사용하여 JSON으로 변환 후 응답 스트림에 작성
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
