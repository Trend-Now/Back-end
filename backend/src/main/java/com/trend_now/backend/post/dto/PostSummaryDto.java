package com.trend_now.backend.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostSummaryDto {
    private Long postId;
    private String title;
    private String writer;
    private int viewCount;
    private Long likeCount;
    private Long commentCount;
    private boolean modifiable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}