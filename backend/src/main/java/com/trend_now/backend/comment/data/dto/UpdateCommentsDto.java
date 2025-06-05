package com.trend_now.backend.comment.data.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UpdateCommentsDto {

    private Long boardId;
    private Long postId;
    private String boardName;
    private Long commentId;
    private String updatedComments;

    public static UpdateCommentsDto of(
            Long boardId, Long postId, String boardName, Long commentId, String updatedComments) {
        return new UpdateCommentsDto(boardId, postId, boardName, commentId, updatedComments);
    }
}
