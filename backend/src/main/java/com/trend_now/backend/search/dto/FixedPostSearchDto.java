package com.trend_now.backend.search.dto;

import com.trend_now.backend.post.dto.PostSummaryDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FixedPostSearchDto {
    private int totalPageCount;
    private long totalCount;
    private List<PostSummaryDto> postList;

    public static FixedPostSearchDto of(int totalPageCount, long totalCount, List<PostSummaryDto> postList) {
        return new FixedPostSearchDto(totalPageCount, totalCount, postList);
    }

}
