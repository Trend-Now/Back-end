package com.trend_now.backend.exception;

import com.trend_now.backend.exception.CustomException.DuplicateException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.exception.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러에서 발생하는 예외를 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Exception 에러가 들어오면 BadRequest(400) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception exception, HttpServletRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    /**
     * IllegalArgumentException 에러가 들어오면 BadRequest(400) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(errorResponseDto);
    }

    /**
     * 존재하지 않는 리소스를 요청하면 NotFound(404) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFoundException(NotFoundException exception, HttpServletRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
    }

    /**
     * 중복이 허용되지 않는 필드에 중복되는 데이터가 들어오면 Conflict(409) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateException(DuplicateException exception, HttpServletRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDto);
    }
}
