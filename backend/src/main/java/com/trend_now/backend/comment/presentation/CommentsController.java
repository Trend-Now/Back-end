package com.trend_now.backend.comment.presentation;

import com.trend_now.backend.comment.application.CommentsService;
import com.trend_now.backend.comment.data.dto.*;
import com.trend_now.backend.comment.data.vo.FindCommentsResponse;
import com.trend_now.backend.comment.data.vo.SaveCommentsRequest;
import com.trend_now.backend.comment.data.vo.UpdateCommentsRequest;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.common.Util;
import com.trend_now.backend.member.domain.Members;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boards/{boardId}/posts/{postId}/comments")
@Tag(name = "Comment API", description = "댓글 관련 API")
public class CommentsController {

    private static final String SUCCESS_SAVE_COMMENT = "댓글 작성에 성공했습니다.";
    private static final String SUCCESS_DELETE_COMMENT = "댓글 삭제에 성공했습니다.";
    private static final String SUCCESS_UPDATE_COMMENT = "댓글 수정에 성공했습니다.";

    private final CommentsService commentsService;
    private final CommentsRepository commentsRepository;

    @Operation(summary = "댓글 저장", description = "게시글에 댓글을 작성합니다.")
    @PostMapping()
    public ResponseEntity<String> saveComments(
            @PathVariable Long boardId
            , @PathVariable Long postId
            , @AuthenticationPrincipal(expression = "members") Members member
            , @RequestBody SaveCommentsRequest saveCommentsRequest) {
        Util.checkMemberExist(member);
        commentsService.saveComments(member, SaveCommentsDto.of(
                boardId, postId, null, saveCommentsRequest.getContent()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_SAVE_COMMENT);
    }

    @Operation(summary = "댓글 조회", description = "게시글에 댓글을 조회합니다.")
    @GetMapping()
    public ResponseEntity<FindCommentsResponse> findAllCommentsByPostId(@PathVariable Long postId
    , @RequestParam(required = false, defaultValue = "1") int page
    , @RequestParam(required = false, defaultValue = "1") int size) {
        // PageRequest 객체 생성 (page는 0부터 시작하므로 -1)
        Pageable pageable = PageRequest.of(page - 1, size);
        return ResponseEntity.status(HttpStatus.OK)
                .body(commentsService.findAllCommentsByPostId(postId, pageable));
    }

    @Operation(summary = "댓글 삭제", description = "특정 게시판의 BOARD_TTL 만료 시간 안의 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteCommentsByMembersAndCommentId(
            @PathVariable Long boardId
            , @PathVariable Long postId
            , @PathVariable Long commentId
            , @AuthenticationPrincipal(expression = "members") Members member) {
        Util.checkMemberExist(member);
        commentsService.deleteCommentsByCommentId(member, DeleteCommentsDto.of(
                boardId, postId, null, commentId));
        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_DELETE_COMMENT);
    }

    @Operation(summary = "댓글 수정", description = "특정 게시판의 BOARD_TTL 만료 시간 안의 댓글을 수정합니다.")
    @PatchMapping("/{commentId}")
    public ResponseEntity<String> updateCommentsByMembersAndCommentId(
            @PathVariable Long boardId
            , @PathVariable Long postId
            , @PathVariable Long commentId
            , @AuthenticationPrincipal(expression = "members") Members members
            , @RequestBody UpdateCommentsRequest updateCommentsRequest) {
        Util.checkMemberExist(members);
        commentsService.updateCommentsByMembersAndCommentId(members,
                UpdateCommentsDto.of(
                        boardId, postId, null, commentId, updateCommentsRequest.getUpdateContent()
                ));
        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_UPDATE_COMMENT);
    }
}
