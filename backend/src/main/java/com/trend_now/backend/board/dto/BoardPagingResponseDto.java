package com.trend_now.backend.board.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardPagingResponseDto {

    private final long totalPageCount;
    private final long totalBoardCount;
    private List<RealtimeBoardDto> boardInfoDtos;

    public static BoardPagingResponseDto from(long totalPageCount, long totalBoardCount, List<RealtimeBoardDto> boardInfoDtoList) {
        return new BoardPagingResponseDto(totalPageCount, totalBoardCount, boardInfoDtoList);
    }
}
