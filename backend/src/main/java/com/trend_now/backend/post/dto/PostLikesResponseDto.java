package com.trend_now.backend.post.dto;

import com.trend_now.backend.post.domain.PostLikesAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostLikesResponseDto {

    private String message;
    private PostLikesAction postLikesAction;

    public static PostLikesResponseDto of(String message, PostLikesAction postLikesAction) {
        return new PostLikesResponseDto(message, postLikesAction);
    }
}
