package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BoardInfoDto {

    private Long boardId;
    private String boardName;
    private long boardLiveTime;
    private double score;
}
