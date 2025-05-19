package com.trend_now.backend.board.cache;

import com.trend_now.backend.board.domain.BoardCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardCacheEntry {

    private Long boardId;
    private String boardName;
    private String disassembledBoardName;
}
