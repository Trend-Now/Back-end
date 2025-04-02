package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsUpdateDto {

    @NotEmpty(message = "게시글을 선택해주세요.")
    private Long postId;

    @NotEmpty(message = "제목을 입력해주세요.")
    private String title;

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;

    @NotEmpty(message = "작성자을 입력해주세요.")
    private String writer;

    public static PostsUpdateDto of(Long postId, String title, String content, String writer) {
        return new PostsUpdateDto(postId, title, content, writer);
    }
}
