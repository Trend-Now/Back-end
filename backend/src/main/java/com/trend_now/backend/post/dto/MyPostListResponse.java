package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPostListResponse {
    private String message;
    private int totalPageCount;
    private long totalCount;
    private List<PostWithBoardSummaryDto> postListDto;

    public static MyPostListResponse of(String message, int totalPageCount, long totalCount, List<PostWithBoardSummaryDto> postList) {
        return new MyPostListResponse(message, totalPageCount, totalCount, postList);
    }
}
