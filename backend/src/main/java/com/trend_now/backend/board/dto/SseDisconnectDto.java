/*
 * 클래스 설명 : clientId가 사용하는 SSE를 서버(Redis, Repository)로부터 제거할 때 사용되는 클래스
 */
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
