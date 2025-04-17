/*
 * 클래스 설명 : 게시글 저장 DTO
 */
package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsSaveDto {

    @NotEmpty(message = "게시판을 선택해주세요.")
    private Long boardId;

    @NotEmpty(message = "제목을 입력해주세요.")
    private String title;

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;

    private List<Long> imageIds;

    public static PostsSaveDto of(Long boardId, String title, String content, List<Long> imageIds) {
        return new PostsSaveDto(boardId, title, content, imageIds);
    }
}
