package com.trend_now.backend.exception.CustomException;

public class S3FileUploadException extends RuntimeException {
  public S3FileUploadException(String message) {
    super(message);
  }
}
