package com.trend_now.backend.exception.CustomException;

/**
 *  AES 암호화 실패 예외
 */
public class AesEncryptionException extends RuntimeException {

    public AesEncryptionException(String message) {
        super(message);
    }
}
