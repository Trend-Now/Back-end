package com.trend_now.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostInfoResponseDto {
    private String message;
    private PostsInfoDto postInfoDto;

    public static PostInfoResponseDto of(String message, PostsInfoDto postInfoDto) {
        return new PostInfoResponseDto(message, postInfoDto);
    }

}
