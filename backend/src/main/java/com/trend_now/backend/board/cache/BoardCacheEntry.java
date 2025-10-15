package com.trend_now.backend.board.cache;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardCacheEntry {

    private String boardName;
    private Set<String> splitBoardNameByBlank; // 문자열 유사도 비교를 위한 Set
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
