package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignalKeywordEventDto {

    private String clientId;
    private String message;
    private SignalKeywordDto signalKeywordDto;
}
