package com.trend_now.backend.exception.customException;

/**
 * 게시판 만료일 때, 발생하는 예외
 */
public class BoardExpiredException extends RuntimeException {

    public BoardExpiredException(String message) {
        super(message);
    }
}
