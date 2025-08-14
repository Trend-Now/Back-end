package com.trend_now.backend.board.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class RealtimeBoardDto {
    private Long boardId;
    private String boardName;
    private Long postCount;
    private Long viewCount;
    @Setter
    private Long boardLiveTime;
    @Setter
    private Double score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JPQL Projection Constructor
    public RealtimeBoardDto(Long boardId, String boardName, Long postCount, Long viewCount, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.postCount = postCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
