package com.trend_now.backend.comment.data.dto;

import com.trend_now.backend.comment.domain.BoardTtlStatus;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@ToString
public class FindAllCommentsDto {

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long id;
    private final String content;
    private final BoardTtlStatus boardTtlStatus;
}
