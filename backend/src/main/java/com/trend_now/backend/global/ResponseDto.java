package com.trend_now.backend.global;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ResponseDto<T> {

    @JsonProperty("isSuccess")
    private final boolean isSuccess;
    private final String message;
    private final HttpStatus statusCode;

    // data 필드는 null일 경우 JSON 직렬화에서 제외
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    // 성공 시 응답: result 데이터 포함
    public static <T> ResponseDto<T> onSuccess(T data, String message) {
        return new ResponseDto<>(true, message, HttpStatus.OK, data);
    }

    // 성공 시 응답: result 데이터 미포함 (e.g., 생성, 수정, 삭제)
    public static <T> ResponseDto<T> onSuccess(String message) {
        return new ResponseDto<>(true, message, HttpStatus.OK, null);
    }

    // 실패 시 응답
    public static <T> ResponseDto<T> onFailure(HttpStatus code, String message) {
        return new ResponseDto<>(false, message, code, null);
    }

}
