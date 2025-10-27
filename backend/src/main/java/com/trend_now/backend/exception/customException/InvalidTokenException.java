package com.trend_now.backend.exception.customException;

import org.springframework.security.core.AuthenticationException;

/**
 *  JWT 관련 예외
 */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
