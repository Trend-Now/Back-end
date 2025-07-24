package com.trend_now.backend.comment.data.dto;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@ToString
public class FindAllCommentsDto {

    private final Long commentId;
    private final String writer;
    private final Long writerId;
    private final String content;
    private final boolean modifiable;
    private final boolean isMyComments;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // 기본 댓글 정보 JPQL 프로젝션용 생성자
    public FindAllCommentsDto(LocalDateTime createdAt, LocalDateTime updatedAt, Long commentId, String content
            , boolean modifiable, String writer, Long writerId) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.commentId = commentId;
        this.content = content;
        this.modifiable = modifiable;
        this.writer = writer;
        this.writerId = writerId;
        this.isMyComments = false;
    }
}
