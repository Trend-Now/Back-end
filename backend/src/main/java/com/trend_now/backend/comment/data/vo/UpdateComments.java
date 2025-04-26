package com.trend_now.backend.comment.data.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class UpdateComments {

    private final Long postId;
    private final Long boardId;
    private final String boardName;
    private final Long commentId;
    private final String updatedComments;
}
