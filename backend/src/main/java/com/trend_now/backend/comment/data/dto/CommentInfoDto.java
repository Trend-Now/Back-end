package com.trend_now.backend.comment.data.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CommentInfoDto {
    private Long postId;
    private String postTitle;
    private Long commentId;
    private String content;
    private String nickname;
    //    private int likeCount;
    private LocalDateTime createdAt;
}
