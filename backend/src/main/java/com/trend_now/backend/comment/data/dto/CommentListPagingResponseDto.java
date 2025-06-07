package com.trend_now.backend.comment.data.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CommentListPagingResponseDto {
    private String message;
    private int totalPageCount;
    private List<CommentInfoDto> commentsInfoListDto;
}
