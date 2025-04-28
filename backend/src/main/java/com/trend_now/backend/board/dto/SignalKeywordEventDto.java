/*
 * 클래스 설명 : SSE 이벤트가 발행되었을 때 누구의 SSE 인지(clientId), SSE의 용도(message)는 무엇인지,
 *             실시간 검색어 순위(signalKeywordDto)는 어떻게 되는지 알고 싶을 때 사용되는 dto
 */
package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignalKeywordEventDto {

    private String clientId;
    private String message;
    private Top10WithChange top10WithChange;
}
