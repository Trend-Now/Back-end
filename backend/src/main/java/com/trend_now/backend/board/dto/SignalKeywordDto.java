/*
 * 클래스 설명 : 시그널 검색어 순위가 언제 갱신된 데이터(now)인지, 순위(top10)는 어떻게 되는지 확인할 때 사용되는 dto
 */
package com.trend_now.backend.board.dto;

import java.util.List;
import lombok.Data;

@Data
public class SignalKeywordDto {

    private long now;
    private List<Top10> top10;
}
