package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RealtimePostSearchDto {
    private int totalPageCount;
    private long totalCount;
    private List<PostWithBoardSummaryDto> realtimePostList;

    public static RealtimePostSearchDto of(int totalPageCount, long totalCount, List<PostWithBoardSummaryDto> realtimePostList) {
        return new RealtimePostSearchDto(totalPageCount, totalCount, realtimePostList);
    }
}
