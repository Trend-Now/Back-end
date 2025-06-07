package com.trend_now.backend.board.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardAutoCompleteResponseDto {

    private String message;
    private List<BoardInfoDto> boardList;

    public static BoardAutoCompleteResponseDto from(List<BoardInfoDto> boardList) {
        return BoardAutoCompleteResponseDto.builder()
                .message("게시판 자동완성 조회 성공")
                .boardList(boardList)
                .build();
    }
}