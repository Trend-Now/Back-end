package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RealTimeBoardKeyExpiredEvent {

    private String boardName;

    public static RealTimeBoardKeyExpiredEvent of(String boardName) {
        return new RealTimeBoardKeyExpiredEvent(boardName);
    }
}
