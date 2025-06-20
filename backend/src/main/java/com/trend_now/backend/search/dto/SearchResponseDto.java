package com.trend_now.backend.search.dto;

import com.trend_now.backend.board.dto.BoardSummaryDto;
import com.trend_now.backend.post.dto.PostListResponseDto;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.RealtimePostSearchDto;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchResponseDto {

    private String message;

    // 실시간 인기 게시판 - 제목
    private List<BoardSummaryDto> realtimeBoardList;

    // 실시간 인기 게시글 - 제목
    private RealtimePostSearchDto realtimePostList;

    // 전날 인기 게시판 - 제목
    private List<String> yesterdayBoardList;

    // 전날 인기 게시글 - 제목
    private List<PostSummaryDto> yesterdayPostList;

    // 고정 게시판 - 제목
    private List<BoardSummaryDto> fixedBoardList;

    // 고정 게시글 - 제목, 내용
    private Map<String, PostListResponseDto> fixedPostList;
}
