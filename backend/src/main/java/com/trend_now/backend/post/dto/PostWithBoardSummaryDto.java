package com.trend_now.backend.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostWithBoardSummaryDto {
    private Long postId;
    private String title;
    private String writer;
    private int viewCount;
    private Long commentCount;
    private Long likeCount;
    private boolean modifiable;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Long boardId;
    private String boardName;
}
