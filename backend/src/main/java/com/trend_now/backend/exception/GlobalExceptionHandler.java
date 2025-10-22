package com.trend_now.backend.exception;

import com.trend_now.backend.exception.customException.DuplicateException;
import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.exception.customException.UnauthorizedException;
import com.trend_now.backend.exception.dto.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러에서 발생하는 예외를 처리한다.
 */
@RestControllerAdvice
@Hidden // Swagger가 이 클래스를 문서화하지 않도록 설정
public class GlobalExceptionHandler {

    public static final String HEADER_ACCEPT = "Accept";
    public static final String TEXT_EVENT_STREAM = "text/event-stream";

    /**
     * Exception 에러가 들어오면 BadRequest(400) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception exception, HttpServletRequest request) {
        if (request.getHeader(HEADER_ACCEPT) != null && request.getHeader(HEADER_ACCEPT).contains(TEXT_EVENT_STREAM)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }

    /**
     * RuntimeException 에러가 들어오면 InternalServerError(500) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException exception, HttpServletRequest request) {
        if (request.getHeader(HEADER_ACCEPT) != null && request.getHeader(HEADER_ACCEPT).contains(TEXT_EVENT_STREAM)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorizedException(UnauthorizedException exception, HttpServletRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
    }

    /**
     * 시스템 상태가 요청을 수행할 수 없는 경우 Forbidden(403) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalStateException(IllegalStateException exception, HttpServletRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.FORBIDDEN,
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

    /**
     * @Valid 또는 @Validated 검증이 실패하면 BadRequest(400) 상태코드와 함께 에러 메시지를 반환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public  ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String errorMessage = exception.getBindingResult()
            .getAllErrors()
            .stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST,
                errorMessage,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
    }
}
