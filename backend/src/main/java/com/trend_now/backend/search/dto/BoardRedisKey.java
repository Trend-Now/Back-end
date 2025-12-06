package com.trend_now.backend.search.dto;

import com.trend_now.backend.board.dto.BoardKeyProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardRedisKey implements BoardKeyProvider {

    private Long boardId;
    private String boardName;
}
