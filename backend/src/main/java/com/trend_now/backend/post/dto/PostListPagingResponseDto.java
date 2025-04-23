/*
 * 클래스 설명 : 게시글 페이징 반환 DTO
 */
package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
public class PostListPagingResponseDto {

    private String message;
    private List<PostListDto> postsInfoListDto;

    public static PostListPagingResponseDto of(String message, List<PostListDto> postsInfoListDto) {
        return new PostListPagingResponseDto(message, postsInfoListDto);
    }
}
