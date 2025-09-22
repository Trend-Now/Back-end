package com.trend_now.backend.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class CookieUtil {

    private static final String REFERER = "Referer";
    private static final String PREFIX_HTTPS = "https";
    private static final String SET_COOKIE = "Set-Cookie";
    private static final String LOCALHOST = "localhost";
    private static final String DOMAIN = ".trendnow.me";

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Cookie 저장
     * - 로컬 환경(http or localhost)과 배포 환경(https) 분기 처리하여 Cookie 저장 속성 지정
     * - 해당 정보는 HTTP Referer 정보를 기반으로 판단
     */
    public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
        int maxAge) {
        String sourceUrl = request.getHeader(REFERER);
        boolean isProd = isProductionEnvironment(sourceUrl);

        log.info("[CookieUtil.addCookie] sourceUrl = {}, 요청 환경 = {}", sourceUrl, isProd ? "배포 환경" : "개발 환경");

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge)
                .secure(isProd)
                .sameSite(isProd ? "None" : "Lax")
                .domain(DOMAIN)
                .build();

        response.addHeader(SET_COOKIE, cookie.toString());
    }

    private static boolean isProductionEnvironment(String sourceUrl) {
        return sourceUrl != null
                && sourceUrl.startsWith(PREFIX_HTTPS)
                && !isLocalEnvironment(sourceUrl);
    }

    private static boolean isLocalEnvironment(String sourceUrl) {
        return sourceUrl.contains(LOCALHOST) ||
                sourceUrl.contains("127.0.0.1") ||
                sourceUrl.contains("0.0.0.0");
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response,
        String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }
}
