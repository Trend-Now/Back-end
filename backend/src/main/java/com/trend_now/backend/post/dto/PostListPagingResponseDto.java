/*
 * 클래스 설명 : 게시글 페이징 반환 DTO
 */
package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostListPagingResponseDto {

    private final String message;
    private final List<PostListDto> postsInfoListDto;

    public static PostListPagingResponseDto of(String message, List<PostListDto> postsInfoListDto) {
        return new PostListPagingResponseDto(message, postsInfoListDto);
    }
}
