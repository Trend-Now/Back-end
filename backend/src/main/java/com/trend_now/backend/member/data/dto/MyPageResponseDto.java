package com.trend_now.backend.member.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageResponseDto {

    private String nickname;
    private String email;
    private String profileImageUrl;

    public static MyPageResponseDto of(String nickname, String email, String profileImageUrl) {
        return new MyPageResponseDto(nickname, email, profileImageUrl);
    }
}