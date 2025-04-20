package com.trend_now.backend.comment.data.vo;

import com.trend_now.backend.comment.domain.BoardTtlStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class FindAllComments {

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long id;
    private final String content;
    private final BoardTtlStatus boardTtlStatus;
}
