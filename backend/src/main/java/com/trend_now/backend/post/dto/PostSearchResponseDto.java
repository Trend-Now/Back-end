package com.trend_now.backend.post.dto;

import com.trend_now.backend.board.dto.BoardInfoDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchResponseDto {

    private String message;

    // 실시간 인기 게시판 - 제목
    private List<BoardInfoDto> boardTitleList;

    // 실시간 인기 게시글 - 제목
    private List<PostSummaryDto> postList;

    // 전날 인기 게시판 - 제목
    private List<String> yesterdayBoardTitleList;

    // 전날 인기 게시글 - 제목
    private List<PostSummaryDto> yesterdayPostList;

    // 고정 게시판 - 제목
    private List<BoardInfoDto> fixedBoardTitleList;

    // 고정 게시글 - 제목, 내용
    private List<PostSummaryDto> fixedPostList;
}
