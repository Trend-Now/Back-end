package com.trend_now.backend.comment.data.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentListPagingResponseDto {
    private String message;
    private int totalPageCount;
    private List<CommentInfoDto> commentsInfoListDto;

    public static CommentListPagingResponseDto of(String message, int totalPageCount, List<CommentInfoDto> commentsInfoListDto) {
        return new CommentListPagingResponseDto(message, totalPageCount, commentsInfoListDto);
    }
}
