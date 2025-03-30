package com.trend_now.backend.member.data.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  구글에서 전달받은 Access Token 정보를 매칭하는 클래스
 */
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)     // 없는 필드는 자동으로 무시
public class AccessToken {

    // 구글이 제공해주는 데이터이기 때문에, JSON 프로퍼티와 동일하게 변수명을 가져야 한다.
    private final String access_token;
    private final String expires_in;     // Access Token 만료 시간
    private final String scope;     // 유저 정보의 범위
    private final String id_token;       // 사용자 정보를 JWT 토큰으로 만든 것
}
