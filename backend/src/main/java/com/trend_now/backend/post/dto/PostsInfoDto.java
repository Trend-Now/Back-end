/*
 * 클래스 설명 : 게시글 정보 DTO
 */
package com.trend_now.backend.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostsInfoDto {
    private final String title;
    private final String writer;
    private final String content;
    private final int viewCount;
    private final Long likeCount;
    private final Long commentCount;
    private final boolean modifiable;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
