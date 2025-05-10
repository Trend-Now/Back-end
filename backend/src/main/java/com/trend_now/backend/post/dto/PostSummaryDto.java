package com.trend_now.backend.post.dto;

import com.trend_now.backend.post.domain.Posts;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PostSummaryDto {

    private Long postId;
    private String title;
    private String writer;
    private int viewCount;
    private int likeCount;
    private LocalDateTime updatedAt;

    public static PostSummaryDto of(Posts posts, int likeCount) {
        return new PostSummaryDto(posts.getId(), posts.getTitle(), posts.getWriter(),
            posts.getViewCount(), likeCount, posts.getUpdatedAt());
    }

    private PostSummaryDto(Long postId, String title, String writer, int viewCount, int likeCount, LocalDateTime updatedAt) {
        this.postId = postId;
        this.title = title;
        this.writer = writer;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.updatedAt = updatedAt;
    }
}