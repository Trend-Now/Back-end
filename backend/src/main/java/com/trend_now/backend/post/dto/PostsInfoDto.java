/*
 * 클래스 설명 : 게시글 정보 DTO
 */
package com.trend_now.backend.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@AllArgsConstructor
public class PostsInfoDto {
    private final String title;
    private final String writer;
    private final String content;
    private int viewCount;
    private int likeCount;
    private final Long commentCount;
    private final boolean modifiable;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PostsInfoDto(String title, String writer, String content, Long commentCount,
        boolean modifiable, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.title = title;
        this.writer = writer;
        this.content = content;
        this.commentCount = commentCount;
        this.modifiable = modifiable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
