package com.trend_now.backend.post.dto;

import com.trend_now.backend.post.domain.Posts;
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
    private int likeCount;
    private boolean modifiable;
    private LocalDateTime updatedAt;

    public static PostSummaryDto of(Posts posts, int likeCount) {
        return new PostSummaryDto(posts.getId(), posts.getTitle(), posts.getWriter(),
            posts.getViewCount(), likeCount, posts.isModifiable(), posts.getUpdatedAt());
    }
}