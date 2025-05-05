package com.trend_now.backend.comment.data.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class SaveCommentsDto {

    /**
     * 댓글에 게시판 식별자와 게시판 이름이 필요한 이유
     * - CommentsService에서 사용되는 BOARD_TTL 획득을 위한 key가 "boardName:boardId" 이기 때문에 데이터를 받아와야 한다.
     * - ex. 트렌드나우:4
     */
    private final Long postId;
    private final Long boardId;
    private final String boardName;
    private final String content;
}
