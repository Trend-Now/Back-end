package com.trend_now.backend.comment.presentation;

import com.trend_now.backend.comment.application.CommentsService;
import com.trend_now.backend.comment.data.vo.SaveComments;
import com.trend_now.backend.member.domain.Members;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
@Tag(name = "Comment API", description = "댓글 관련 API")
public class CommentsController {

    private static final String SUCCESS_SAVE_COMMENT = "댓글 작성에 성공했습니다.";

    private final CommentsService commentsService;

    @Operation(summary = "댓글 저장", description = "게시글에 댓글을 작성합니다.")
    @PostMapping()
    public ResponseEntity<String> saveComments(@AuthenticationPrincipal(expression = "members") Members member
            , @RequestBody SaveComments saveComments) {
        commentsService.saveComments(member, saveComments);
        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_SAVE_COMMENT);
    }
}
