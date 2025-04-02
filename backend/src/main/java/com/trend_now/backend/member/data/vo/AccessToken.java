package com.trend_now.backend.member.data.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  OAuth2 서버에서 전달받은 Access Token 정보를 매칭하는 클래스
 *  - OAuth2 제공해주는 데이터이기 때문에, JSON 프로퍼티와 동일하게 변수명을 가져야 한다.
 *  - 해당 프로퍼티들은 OAuth2 기업 공통이라 하나의 클래스로 처리한다.
 */
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)     // 없는 필드는 자동으로 무시
public class AccessToken {

    private final String access_token;
    private final String expires_in;     // Access Token 만료 시간
    private final String scope;     // 유저 정보의 범위
    private final String id_token;       // 사용자 정보를 JWT 토큰으로 만든 것
}
