package com.trend_now.backend.member.data.vo;

import lombok.*;

/**
 *  구글 로그인 시, 반환되는 데이터
 */
@Data
@AllArgsConstructor
@Builder
public class GoogleLoginResponse {

    private final Long memberId;
    private final String jwt;
}
