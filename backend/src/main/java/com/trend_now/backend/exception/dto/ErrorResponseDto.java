package com.trend_now.backend.exception.dto;

import org.springframework.http.HttpStatus;

public record ErrorResponseDto(
        HttpStatus statusCode,
        String errorMessage,
        String Path
) {
}
