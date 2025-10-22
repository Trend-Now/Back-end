/*
 * 클래스 설명 : 게시글 저장 DTO
 */
package com.trend_now.backend.post.dto;

import com.trend_now.backend.common.annotation.ValidContent;
import com.trend_now.backend.post.domain.Posts;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostsSaveDto {

    @NotEmpty(message = "제목을 입력해주세요.")
    @Size(min = 1, max = Posts.MAX_TITLE_LENGTH, message = "제목은 100자를 초과할 수 없습니다.")
    private final String title;

    @NotEmpty(message = "내용을 입력해주세요.")
    @ValidContent
    private final String content;

    @Size(max = 10, message = "이미지는 10장을 초과할 수 없습니다.")
    private final List<Long> imageIds;

    public static PostsSaveDto of(String title, String content, List<Long> imageIds) {
        return new PostsSaveDto(title, content, imageIds);
    }
}
