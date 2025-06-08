package com.trend_now.backend.search.dto;

import com.trend_now.backend.board.dto.BoardSummaryDto;
import com.trend_now.backend.post.dto.PostSummaryDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchResponseDto {

    private String message;

    // 실시간 인기 게시판 - 제목
    private List<BoardSummaryDto> boardList;

    // 실시간 인기 게시글 - 제목
    private List<PostSummaryDto> postList;

    // 전날 인기 게시판 - 제목
    private List<String> yesterdayBoardList;

    // 전날 인기 게시글 - 제목
    private List<PostSummaryDto> yesterdayPostList;

    // 고정 게시판 - 제목
    private List<BoardSummaryDto> fixedBoardList;

    // 고정 게시글 - 제목, 내용
    private List<PostSummaryDto> fixedPostList;
}
