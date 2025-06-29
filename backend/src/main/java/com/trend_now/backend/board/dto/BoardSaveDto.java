package com.trend_now.backend.board.dto;

import com.trend_now.backend.board.application.BoardKeyProvider;
import com.trend_now.backend.board.domain.BoardCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardSaveDto implements BoardKeyProvider {

    private Long boardId;
    private String name;
    private BoardCategory boardCategory;

    public static BoardSaveDto from(Top10 top10) {
        return new BoardSaveDto(-1L, top10.getKeyword(), BoardCategory.REALTIME);
    }
}
