package com.trend_now.backend.comment.data.dto;

import com.trend_now.backend.member.domain.Members;
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
    private final boolean modifiable;

    // 댓글 작성자
    private final String writer;

    // 댓글 작성자 식별자
    private final Long writerId;
}
