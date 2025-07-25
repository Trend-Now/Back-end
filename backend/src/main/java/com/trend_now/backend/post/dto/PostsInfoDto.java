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
    private final String content;
    private final Long writerId;
    private final String writer;
    private int viewCount;
    private int likeCount;
    private boolean isMyPost;
    private boolean isScraped;
    private final Long commentCount;
    private final boolean modifiable;
    private final String boardName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;


    public PostsInfoDto(String title, String writer, String content, Long commentCount,
        boolean modifiable, String boardName, LocalDateTime createdAt, LocalDateTime updatedAt, Long writerId) {
        this.title = title;
        this.writer = writer;
        this.content = content;
        this.commentCount = commentCount;
        this.modifiable = modifiable;
        this.boardName = boardName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.writerId = writerId;
    }
}
