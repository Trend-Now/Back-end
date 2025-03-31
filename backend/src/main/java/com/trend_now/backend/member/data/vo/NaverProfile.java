package com.trend_now.backend.member.data.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  Naver 사용자 정보 형식
 *  {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "aa",
 *     "email": "aa@naver.com",
 *     "name": "박찬웅"
 *   }
 * }
 *
 * todo. 필요한 사용자 정보는 추가 가능
 */
@AllArgsConstructor
@Data
// JsonIgnoreProperties 어노테이션을 사용하면 응답 받는 JSON 프로퍼티 중 여기에 정의되지 않은 것들에 의한 에러를 막아준다.
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverProfile {

    private final String resultcode;       // 성공하면 "00"을 반환, 나머지는 실패 또는 오류
    private final String message;
    private final Response response;

    @AllArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private final String id;        // sns는 social_id를 의미
        private final String email;
        private final String name;
    }
}
