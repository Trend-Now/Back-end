package com.trend_now.backend.board.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class Top10 {

    @NotEmpty(message = "순위를 입력해주세요.")
    private int rank;

    @NotEmpty(message = "키워드를 입력해주세요.")
    private String keyword;
}
