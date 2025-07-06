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

    // 댓글 작성자 본인 여부
    private final boolean isMyComments;

    // 기본 댓글 정보 JPQL 프로젝션용 생성자
    public FindAllCommentsDto(LocalDateTime createdAt, LocalDateTime updatedAt, Long id, String content
            , boolean modifiable, String writer, Long writerId) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.id = id;
        this.content = content;
        this.modifiable = modifiable;
        this.writer = writer;
        this.writerId = writerId;
        this.isMyComments = false;
    }
}
