/*
 * 클래스 설명 : 게시글 페이징 요청 DTO
 */
package com.trend_now.backend.post.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostsPagingRequestDto {

    @NotEmpty(message = "게시판을 선택해주세요.")
    private final Long boardId;

    @NotEmpty(message = "페이지를 선택해주세요.")
    private final int page;

    @NotEmpty(message = "페이지의 게시글 개수를 선택해주세요.")
    private final int size;

    public static PostsPagingRequestDto of(Long boardId, int page, int size) {
        return new PostsPagingRequestDto(boardId, page, size);
    }
}
