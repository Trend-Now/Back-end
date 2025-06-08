package com.trend_now.backend.member.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageResponseDto {
    private String nickname;
    private String email;

    public static MyPageResponseDto of(String nickname, String email) {
        return new MyPageResponseDto(nickname, email);
    }
}