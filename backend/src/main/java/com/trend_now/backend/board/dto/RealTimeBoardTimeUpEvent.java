package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RealTimeBoardTimeUpEvent {

    private String boardName;
    private Long timeUp;

    public static RealTimeBoardTimeUpEvent from(String boardName, Long timeUp) {
        return new RealTimeBoardTimeUpEvent(boardName, timeUp);
    }
}
