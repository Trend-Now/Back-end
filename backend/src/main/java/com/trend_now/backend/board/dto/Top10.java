/*
 * 클래스 설명 : 시그널 검색어 순위를 호출했을 때 받아오는 데이터 (실시간 검색어 하나를 의미)
 */
package com.trend_now.backend.board.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Top10 {

    @NotEmpty(message = "순위를 입력해주세요.")
    private int rank;

    @NotEmpty(message = "키워드를 입력해주세요.")
    private String keyword;
    private String state; // 상승, 하락, 유지, 신규
}
