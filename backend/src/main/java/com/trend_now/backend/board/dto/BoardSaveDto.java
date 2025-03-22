package com.trend_now.backend.board.dto;

import com.trend_now.backend.board.domain.BoardCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardSaveDto {

    private String name;
    private BoardCategory boardCategory;

    public static BoardSaveDto from(Top10 top10) {
        return new BoardSaveDto(top10.getKeyword(), BoardCategory.REALTIME);
    }
}
