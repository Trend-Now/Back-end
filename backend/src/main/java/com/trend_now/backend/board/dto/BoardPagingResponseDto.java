package com.trend_now.backend.board.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardPagingResponseDto {

    private List<BoardInfoDto> boardInfoDtos;

    public static BoardPagingResponseDto from(List<BoardInfoDto> boardInfoDtos) {
        return new BoardPagingResponseDto(boardInfoDtos);
    }
}
