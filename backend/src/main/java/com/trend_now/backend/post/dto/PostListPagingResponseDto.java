/*
 * 클래스 설명 : 게시글 페이징 반환 DTO
 */
package com.trend_now.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
public class PostListPagingResponseDto {

    private String message;
    private Page<PostListDto> postsInfoDtos;

    public static PostListPagingResponseDto of(String message, Page<PostListDto> postsInfoDtos) {
        return new PostListPagingResponseDto(message, postsInfoDtos);
    }
}
