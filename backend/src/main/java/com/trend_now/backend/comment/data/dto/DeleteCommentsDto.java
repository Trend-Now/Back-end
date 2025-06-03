package com.trend_now.backend.comment.data.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DeleteCommentsDto {

    private Long boardId;
    private Long postId;
    private String boardName;
    private Long commentId;

    public static DeleteCommentsDto of(Long boardId, Long postId, String boardName, Long commentId) {
        return new DeleteCommentsDto(boardId, postId, boardName, commentId);
    }
}