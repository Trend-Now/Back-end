package com.trend_now.backend.board.dto;

import com.trend_now.backend.board.domain.BoardCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class BoardSaveDto implements BoardKeyProvider {

    private Long boardId;
    private String boardName;
    private BoardCategory boardCategory;
    private int rank;

    public static BoardSaveDto from(Top10 top10) {
        return new BoardSaveDto(-1L, top10.getKeyword(), BoardCategory.REALTIME, top10.getRank());
    }
}
