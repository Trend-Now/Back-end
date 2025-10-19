package com.trend_now.backend.board.dto;

import com.trend_now.backend.board.application.BoardKeyProvider;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardIsDeletedDto implements BoardKeyProvider {

    private Long boardId;
    private String boardName;

    public static BoardIsDeletedDto of(Long boardId, String boardName) {
        return new BoardIsDeletedDto(boardId, boardName);
    }
}
