package com.trend_now.backend.post.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class PostWithBoardSummaryDto {
    private Long postId;
    private String title;
    private String writer;
    @Setter
    private int viewCount;
    private Long commentCount;
    @Setter
    private int likeCount;
    private boolean modifiable;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Long boardId;
    private String boardName;

    // PostsRepository - findByMemberId의 DTO Projection 생성자
    public PostWithBoardSummaryDto(Long postId, String title, String writer, Long commentCount, boolean modifiable, LocalDateTime createAt, LocalDateTime updateAt,
        Long boardId, String boardName) {
        this.postId = postId;
        this.title = title;
        this.writer = writer;
        this.commentCount = commentCount;
        this.modifiable = modifiable;
        this.createAt = createAt;
        this.updateAt = updateAt;
        this.boardId = boardId;
        this.boardName = boardName;
    }
}
