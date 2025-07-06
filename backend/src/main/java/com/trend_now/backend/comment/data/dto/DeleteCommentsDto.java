package com.trend_now.backend.comment.data.dto;

import com.trend_now.backend.board.application.BoardKeyProvider;
import lombok.*;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DeleteCommentsDto implements BoardKeyProvider {

    private Long boardId;
    private Long postId;
    @Setter
    private String boardName;
    private Long commentId;

    public static DeleteCommentsDto of(Long boardId, Long postId, String boardName, Long commentId) {
        return new DeleteCommentsDto(boardId, postId, boardName, commentId);
    }
}