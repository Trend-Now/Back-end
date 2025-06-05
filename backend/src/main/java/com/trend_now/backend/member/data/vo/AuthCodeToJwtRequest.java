package com.trend_now.backend.member.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  프론트엔드에서 인가코드를 통해 JWT 토큰을 반환받을 때, 사용되는 클래스
 */
@AllArgsConstructor
@Data
public class AuthCodeToJwtRequest {

    private final String code;
}
