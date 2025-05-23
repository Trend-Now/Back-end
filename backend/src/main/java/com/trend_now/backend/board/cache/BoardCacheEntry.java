package com.trend_now.backend.board.cache;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardCacheEntry {

    private String boardName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
