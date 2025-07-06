package com.trend_now.backend.comment.data.vo;

import com.trend_now.backend.comment.data.dto.FindAllCommentsDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class FindCommentsResponse {

    private final int totalCommentsCount;
    private final int totalPageCount;
    private final List<FindAllCommentsDto> findAllCommentsDtos;
}
