package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
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

    private List<Long> updateImageIds;

    public static PostsUpdateDto of(Long postId, String title, String content, List<Long> updateImageIds) {
        return new PostsUpdateDto(postId, title, content, updateImageIds);
    }
}
