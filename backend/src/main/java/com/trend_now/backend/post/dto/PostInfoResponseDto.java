package com.trend_now.backend.post.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostInfoResponseDto {

    private final String message;
    private final PostsInfoDto postInfoDto;

    public static PostInfoResponseDto of(String message, PostsInfoDto postInfoDto) {
        return new PostInfoResponseDto(message, postInfoDto);
    }
}
