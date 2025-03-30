package com.trend_now.backend.member.data.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  구글에서 전달받은 사용자 정보를 매칭하는 클래스
 */
@AllArgsConstructor
@Data
// JsonIgnoreProperties 어노테이션을 사용하면 응답 받는 JSON 프로퍼티 중 여기에 정의되지 않은 것들에 의한 에러를 막아준다.
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleProfile {

    private final String sub;       // sub은 open_id(=social_id)를 의미
    private final String email;
    private final String picture;
}
