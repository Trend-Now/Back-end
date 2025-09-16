package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Top10WithDiff {

    private int rank;
    private String keyword;
    @Setter
    private Long boardId;
    private RankChangeType rankChangeType;
    private Integer diffRank;

    public Top10WithDiff(int rank, String keyword, RankChangeType rankChangeType,
        Integer diffRank) {
        this.rank = rank;
        this.keyword = keyword;
        this.rankChangeType = rankChangeType;
        this.diffRank = diffRank;
        this.boardId = null;
    }

    public static Top10WithDiff from(String realtimeKeyword) {
        // realtimeKeyword: "순위:검색어:게시판아이디:상태:등락폭"
        String[] split = realtimeKeyword.split(":");
        return new Top10WithDiff(Integer.parseInt(split[0]), split[1], Long.parseLong(split[2]),
            RankChangeType.fromSymbol(split[3]), Integer.parseInt(split[4]));
    }
}
