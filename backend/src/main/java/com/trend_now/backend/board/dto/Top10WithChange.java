package com.trend_now.backend.board.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Top10WithChange {

    private long now;
    private List<Top10WithDiff> top10WithDiff;
    private List<Long> top10BoardIds;
}
