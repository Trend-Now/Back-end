package com.trend_now.backend.comment.data.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class DeleteComments {

    private final Long postId;
    private final Long boardId;
    private final String boardName;
}