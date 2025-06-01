package com.trend_now.backend.exception.CustomException;

/**
 *  요청 클라이언트와 매칭이 안되는 요청인 경우, 사용하는 예외
 *  - ex. 자신이 작성한 댓글이 아닌데, 삭제 요청한 경우
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
