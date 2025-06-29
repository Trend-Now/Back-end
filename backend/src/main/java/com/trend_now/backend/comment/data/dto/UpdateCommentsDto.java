package com.trend_now.backend.comment.data.dto;

import com.trend_now.backend.board.application.BoardKeyProvider;
import lombok.*;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UpdateCommentsDto implements BoardKeyProvider {

    private Long boardId;
    private Long postId;
    @Setter
    private String boardName;
    private Long commentId;
    private String updatedComments;

    public static UpdateCommentsDto of(
            Long boardId, Long postId, String boardName, Long commentId, String updatedComments) {
        return new UpdateCommentsDto(boardId, postId, boardName, commentId, updatedComments);
    }

    @Override
    public String getName() {
        return boardName;
    }
}
