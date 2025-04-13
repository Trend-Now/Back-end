package com.trend_now.backend.post.presentation;

import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.application.PostLikesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards/")
@Tag(name = "PostLikes API", description = "게시글 좋아요 관련 API")
public class PostLikesController {

    private static final String SUCCESS_INCREMENT_POSTLIKES_MESSAGE = "좋아요를 증가시키는 데 성공했습니다.";

    private final PostLikesService postLikesService;

    @Operation(summary = "좋아요 증가", description = "게시판에 등록된 게시글의 좋아요를 증가시킵니다.")
    @PostMapping("{boardName}/{boardId}/posts/{postId}/likes")
    public ResponseEntity<String> incrementPostLikes(
            @AuthenticationPrincipal(expression = "members") Members member,
            @PathVariable(value = "boardName") String boardName,
            @PathVariable(value = "boardId") Long boardId,
            @PathVariable(value = "postId") Long postId) {

        log.info("좋아요를 증가시키는 컨트롤러 메서드가 호출되었습니다.");

        postLikesService.increaseLikeLock(boardName, boardId, postId, member.getName());

        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_INCREMENT_POSTLIKES_MESSAGE);
    }

}
