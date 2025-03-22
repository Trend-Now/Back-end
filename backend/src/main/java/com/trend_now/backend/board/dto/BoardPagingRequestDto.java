package com.trend_now.backend.board.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardPagingRequestDto {

    @NotEmpty(message = "페이지를 선택해주세요.")
    private int page;

    @NotEmpty(message = "페이지의 게시글 개수를 선택해주세요.")
    private int size;

//    @NotEmpty(message = "페이지의 정렬 방법을 선택해주세요.")
//    private String sortBy;
}
