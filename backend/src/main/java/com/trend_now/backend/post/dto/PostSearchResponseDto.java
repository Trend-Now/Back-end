package com.trend_now.backend.post.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSearchResponseDto {

    private String message;

    // 실시간 인기 게시판 - 제목
    private List<String> boards;

    // 실시간 인기 게시글 - 제목
    private List<PostListDto> postListDtos;

}
