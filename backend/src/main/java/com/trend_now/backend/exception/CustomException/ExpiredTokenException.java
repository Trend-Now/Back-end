package com.trend_now.backend.exception.CustomException;

/**
 * 만료된 Access Token으로 API 요청 시, 해당 예외가 발생
 */
public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException(String message) {
        super(message);
    }
}
