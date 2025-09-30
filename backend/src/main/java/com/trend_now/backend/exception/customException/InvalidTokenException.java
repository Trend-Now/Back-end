package com.trend_now.backend.exception.customException;

import org.springframework.security.authentication.BadCredentialsException;

/**
 *  JWT 관련 예외
 */
public class InvalidTokenException extends BadCredentialsException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
