package com.trend_now.backend.search.dto;

import com.trend_now.backend.board.dto.BoardKeyProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BoardRedisKey implements BoardKeyProvider {

    private Long boardId;
    private String boardName;

}
