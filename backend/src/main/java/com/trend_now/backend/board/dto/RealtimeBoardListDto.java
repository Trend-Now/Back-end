package com.trend_now.backend.board.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class RealtimeBoardListDto {
    private Long boardId;
    private String boardName;
    private Long postCount;
    @Setter
    private Long boardLiveTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RealtimeBoardListDto(Long boardId, String boardName, Long postCount, LocalDateTime createdAt,
        LocalDateTime updatedAt) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.postCount = postCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
