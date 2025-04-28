package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Top10WithDiff {

    private int rank;
    private String keyword;
    private RankChangeType rankChangeType;
    private Integer previousRank;
}
