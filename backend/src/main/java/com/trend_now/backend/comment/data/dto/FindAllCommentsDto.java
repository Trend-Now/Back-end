package com.trend_now.backend.comment.data.dto;

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

    // 전체 댓글 수
    private final int totalCommentsCount;

    // 전체 댓글 페이지 수
    private final int totalPageCount;

    // 기본 댓글 정보 JPQL 프로젝션용 생성자
    public FindAllCommentsDto(LocalDateTime createdAt, LocalDateTime updatedAt, Long id, String content, boolean modifiable) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.id = id;
        this.content = content;
        this.modifiable = modifiable;

        // count 집계 전에는 기본값 0 부여하고 서비스 계층에서 실제 값 초기화
        this.totalCommentsCount = 0;
        this.totalPageCount = 0;
    }
}
