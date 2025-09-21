package com.trend_now.backend.exception.customException;

/**
 * 중복된 값이 존재할 때 발생하는 예외
 */
public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }
}
