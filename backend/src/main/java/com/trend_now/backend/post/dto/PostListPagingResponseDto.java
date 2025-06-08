/*
 * 클래스 설명 : 게시글 페이징 반환 DTO
 */
package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostListPagingResponseDto {

    private final String message;
    private final int totalPageCount;
    private final List<PostSummaryDto> postsInfoListDto;

    public static PostListPagingResponseDto of(String message, int totalPageCount, List<PostSummaryDto> postsInfoListDto) {
        return new PostListPagingResponseDto(message, totalPageCount, postsInfoListDto);
    }
}
