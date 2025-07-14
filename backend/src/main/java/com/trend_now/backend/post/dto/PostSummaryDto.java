package com.trend_now.backend.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class PostSummaryDto {
    private Long postId;
    private String title;
    private String writer;
    @Setter
    private int viewCount;
    @Setter
    private int likeCount;
    private Long commentCount;
    private boolean modifiable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // PostsRepository - findAllByBoardsId의 DTO Projection 생성자
    public PostSummaryDto(Long postId, String title, String writer, Long commentCount,
        boolean modifiable, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.postId = postId;
        this.title = title;
        this.writer = writer;
        this.commentCount = commentCount;
        this.modifiable = modifiable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}