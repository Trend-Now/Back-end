package com.trend_now.backend.post.presentation;

import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.application.ScrapService;
import com.trend_now.backend.post.domain.ScrapAction;
import com.trend_now.backend.post.dto.ScrapResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards/{boardId}/posts/{postId}/scrap")
@Tag(name = "Post API", description = "게시글 관련 API")
public class ScrapController {

    private final ScrapService scrapService;

    private static final String SUCCESS_SCRAPPED_POST_MESSAGE = "게시글 스크랩 등록 성공";
    private static final String SUCCESS_UNSCRAPPED_POST_MESSAGE = "게시글 스크랩 취소 성공";

    @PostMapping
    @Operation(summary = "게시글 스크랩", description = "게시글을 스크랩합니다.")
    public ResponseEntity<ScrapResponseDto> scarpPost(
        @AuthenticationPrincipal(expression = "members") Members members,
        @PathVariable Long boardId,
        @PathVariable Long postId) {

        ScrapAction scrapAction = scrapService.scrapPost(members.getId(), postId);
        String message = scrapAction == ScrapAction.SCRAPPED ? SUCCESS_SCRAPPED_POST_MESSAGE : SUCCESS_UNSCRAPPED_POST_MESSAGE;

        return ResponseEntity.status(HttpStatus.CREATED).body(ScrapResponseDto.of(message, scrapAction));
    }
}
