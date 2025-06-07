package com.trend_now.backend.board.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardSummaryDto {
    private Long boardId;
    private String boardName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
