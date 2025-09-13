package com.trend_now.backend.config.auth.oauth;

import com.trend_now.backend.common.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

// AuthorizationRequestRepository: OAuth 로그인을 시작하기 전, 로그인 요청 정보를 저장해두는 임시 보관소
public class CustomAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    // 인가 요청 정보를 저장할 쿠키 이름
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    // redirect URL을 저장할 쿠키 이름
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_url";

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getAuthorizationRequestFromCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }
        // 인가 요청 정보를 쿠키에 저장
        String serializedAuthRequest = Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(authorizationRequest));
        CookieUtil.addCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serializedAuthRequest, 180);

        // redirect_url를 별도 쿠키로 저장
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (redirectUriAfterLogin != null) {
            CookieUtil.addCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, 180);
        }
    }


    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return getAuthorizationRequestFromCookie(request);
    }

    private OAuth2AuthorizationRequest getAuthorizationRequestFromCookie(HttpServletRequest request) {
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
            .map(cookie -> SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue())))
            .map(OAuth2AuthorizationRequest.class::cast)
            .orElse(null);
    }

    private void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
