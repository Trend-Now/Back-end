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
public class PostListResponseDto {

    private final String message;
    private final int totalPageCount;
    private final long totalCount;
    private final List<PostSummaryDto> postsInfoListDto;

    public static PostListResponseDto of(String message, int totalPageCount, long totalCount, List<PostSummaryDto> postsInfoListDto) {
        return new PostListResponseDto(message, totalPageCount, totalCount, postsInfoListDto);
    }
}
