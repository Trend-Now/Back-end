package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScrapPostListResponse {
    private String message;
    private int totalPageCount;
    private long totalCount;
    private List<PostWithBoardSummaryDto> scrapPostList;

    public static ScrapPostListResponse of(String message, int totalPageCount, long totalCount, List<PostWithBoardSummaryDto> scrapPostList) {
        return new ScrapPostListResponse(message, totalPageCount, totalCount, scrapPostList);
    }
}
