package com.trend_now.backend.post.dto;

import com.trend_now.backend.post.domain.Posts;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PostListDto {

    private Long postId;
    private String title;
    private String writer;
    private int viewCount;
    private int likeCount;
    private LocalDateTime updatedAt;

    // Repository에서 PostListDto를 생성할 때 사용
    public PostListDto(Long id, String title, String writer, int viewCount, LocalDateTime updatedAt) {
        this.postId = id;
        this.title = title;
        this.writer = writer;
        this.viewCount = viewCount;
        this.updatedAt = updatedAt;
    }

    public static PostListDto of(Posts posts, int likeCount) {
        return new PostListDto(posts.getId(), posts.getTitle(), posts.getWriter(),
            posts.getViewCount(), likeCount, posts.getUpdatedAt());
    }
}