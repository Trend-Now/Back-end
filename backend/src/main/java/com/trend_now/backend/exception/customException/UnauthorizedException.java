package com.trend_now.backend.exception.customException;

/**
 *  - 인증되지 않은 사용자가 보호된 리소스에 접근하려고 할 때 발생하는 예외
 *  ex) 로그인하지 않은 사용자가 특정 API에 접근하려고 할 때
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
