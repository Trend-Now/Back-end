package com.trend_now.backend.comment.data.dto;

import com.trend_now.backend.board.application.BoardKeyProvider;
import lombok.*;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class SaveCommentsDto implements BoardKeyProvider {

    /**
     * 댓글에 게시판 식별자와 게시판 이름이 필요한 이유
     * - CommentsService에서 사용되는 BOARD_TTL 획득을 위한 key가 "boardName:boardId" 이기 때문에 데이터를 받아와야 한다.
     * - ex. 트렌드나우:4
     */
    private Long boardId;
    private Long postId;
    @Setter
    private String boardName;
    private String content;

    public static SaveCommentsDto of(Long boardId, Long postId, String boardName, String content) {
        return new SaveCommentsDto(boardId, postId, boardName, content);
    }
}
