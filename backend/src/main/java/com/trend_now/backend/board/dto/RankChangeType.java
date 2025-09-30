package com.trend_now.backend.board.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import lombok.Getter;

/**
 * UP : 이전 순위보다 올라감 DOWN : 이전 순위보다 내려감 SAME : 이전 순위랑 동일 NEW : 이전 순위에 없었음
 */
@Getter
public enum RankChangeType {
    UP("+"),
    DOWN("-"),
    SAME("s"),
    NEW("n");

    private final String symbol;

    RankChangeType(String symbol) {
        this.symbol = symbol;
    }

    /**
     * signal.bz에서 받아온 JSON 데이터의 state 값을 RankChangeType enum으로 변환
     */
    @JsonCreator
    public static RankChangeType fromString(String value) {
        for (RankChangeType type : RankChangeType.values()) {
            if (type.name().equalsIgnoreCase(value) || type.getSymbol().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 랭크 변화 타입입니다: " + value);
    }

}
