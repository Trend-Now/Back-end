package com.trend_now.backend.main.dto;

import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MainPageDto {

    private String memberName;
    private String boardRankValidTime;
    private BoardPagingResponseDto boardPagingResponseDto;

    public static MainPageDto of(String memberName, String boardRankValidTime,
            BoardPagingResponseDto boardPagingResponseDto) {
        return new MainPageDto(memberName, boardRankValidTime, boardPagingResponseDto);
    }
}
