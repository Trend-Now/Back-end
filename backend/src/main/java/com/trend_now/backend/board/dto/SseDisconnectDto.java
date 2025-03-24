package com.trend_now.backend.board.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SseDisconnectDto {

    @NotEmpty(message = "clientId는 필수입니다.")
    private String clientId;
}
