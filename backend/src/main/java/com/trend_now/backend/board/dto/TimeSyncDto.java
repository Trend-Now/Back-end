/*
 * 클래스 설명 : 백엔드와 프론트의 시간 동기화를 위해 현재 서버의 시간을 전송할 때 사용되는 DTO
 */
package com.trend_now.backend.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSyncDto {

    private String boardRankValidTime;
}
