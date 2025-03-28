package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsDeleteDto {

    @NotEmpty(message = "게시글을 선택해주세요.")
    private Long postId;

    @NotEmpty(message = "작성자을 입력해주세요.")
    private String writer;

    public static PostsDeleteDto of(Long postId, String writer) {
        return new PostsDeleteDto(postId, writer);
    }
}
