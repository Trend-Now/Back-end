package com.trend_now.backend.exception.CustomException;

/**
 *  AES 복호호화 실패 예외
 */
public class AesDecryptionException extends RuntimeException {

    public AesDecryptionException(String message) {
        super(message);
    }
}
