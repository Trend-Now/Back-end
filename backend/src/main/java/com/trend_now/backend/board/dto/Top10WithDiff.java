package com.trend_now.backend.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Top10WithDiff {

    private int rank;
    private String keyword;
    private RankChangeType rankChangeType;
    private Integer previousRank;
    @Setter
    private Long boardId;

    public Top10WithDiff(int rank, String keyword, RankChangeType rankChangeType,
            Integer previousRank) {
        this.rank = rank;
        this.keyword = keyword;
        this.rankChangeType = rankChangeType;
        this.previousRank = previousRank;
        this.boardId = null;
    }
}
