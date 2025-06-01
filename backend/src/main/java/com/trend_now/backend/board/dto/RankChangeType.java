package com.trend_now.backend.board.dto;

/**
 * UP : 이전 순위보다 올라감 DOWN : 이전 순위보다 내려감 SAME : 이전 순위랑 동일 NEW : 이전 순위에 없었음
 */
public enum RankChangeType {
    UP, DOWN, SAME, NEW
}
