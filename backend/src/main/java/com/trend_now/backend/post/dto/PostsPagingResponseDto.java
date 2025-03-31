/*
 * 클래스 설명 : 게시글 페이징 반환 DTO
 */
package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsPagingResponseDto {

    private String message;
    private List<PostsInfoDto> postsInfoDtos;

    public static PostsPagingResponseDto of(String message, List<PostsInfoDto> postsInfoDtos) {
        return new PostsPagingResponseDto(message, postsInfoDtos);
    }
}
