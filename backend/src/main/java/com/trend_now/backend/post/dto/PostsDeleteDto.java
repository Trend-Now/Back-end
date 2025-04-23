package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsDeleteDto {

    @NotEmpty(message = "게시글을 선택해주세요.")
    private Long postId;

    public static PostsDeleteDto from(Long postId) {
        return new PostsDeleteDto(postId);
    }
}
