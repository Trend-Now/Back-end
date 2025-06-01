package com.trend_now.backend.comment.data.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DeleteCommentsRequest {

    private final String boardName;
}
