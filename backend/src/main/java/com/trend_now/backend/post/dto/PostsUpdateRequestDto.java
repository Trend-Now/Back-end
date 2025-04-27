package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostsUpdateRequestDto {

    @NotEmpty(message = "제목을 입력해주세요.")
    private final String title;

    @NotEmpty(message = "내용을 입력해주세요.")
    private final String content;

    // 삭제된 이미지 ID 리스트
    private final List<Long> deleteImageIdList;

    // 새로 추가된 이미지 ID 리스트
    private final List<Long> newImageIdList;

    public static PostsUpdateRequestDto of(String title, String content, List<Long> deleteImageIdList, List<Long> newImageIdList) {
        return new PostsUpdateRequestDto(title, content, deleteImageIdList, newImageIdList);
    }
}
