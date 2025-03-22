package com.trend_now.backend.board.dto;

import java.util.List;
import lombok.Data;

@Data
public class SignalKeywordDto {

    private long now;
    private List<Top10> top10;
}
