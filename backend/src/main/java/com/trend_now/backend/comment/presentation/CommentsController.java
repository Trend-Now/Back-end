package com.trend_now.backend.comment.presentation;

import com.trend_now.backend.comment.application.CommentsService;
import com.trend_now.backend.comment.data.dto.DeleteCommentsDto;
import com.trend_now.backend.comment.data.dto.FindAllCommentsDto;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.comment.data.vo.DeleteCommentsRequest;
import com.trend_now.backend.comment.data.vo.SaveCommentsRequest;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.common.Util;
import com.trend_now.backend.member.domain.Members;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
                boardId, postId, saveCommentsRequest.getBoardName(), saveCommentsRequest.getContent()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_SAVE_COMMENT);
    }

    @Operation(summary = "댓글 조회", description = "게시글에 댓글을 조회합니다.")
    @GetMapping()
    public ResponseEntity<List<FindAllCommentsDto>> findAllCommentsByPostId(@PathVariable Long postId) {
        return ResponseEntity.status(HttpStatus.OK).body(commentsRepository.findByPostsIdOrderByCreatedAtDesc(postId));
    }

    @Operation(summary = "댓글 삭제", description = "특정 게시판의 BOARD_TTL 만료 시간 안의 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteCommentsByMembersAndCommentId(
            @PathVariable Long boardId
            , @PathVariable Long postId
            , @PathVariable Long commentId
            , @AuthenticationPrincipal(expression = "members") Members member
            , @RequestBody DeleteCommentsRequest deleteCommentsRequest) {
        Util.checkMemberExist(member);
        commentsService.deleteCommentsByCommentId(member, DeleteCommentsDto.of(
                boardId, postId, deleteCommentsRequest.getBoardName(), commentId));
        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_DELETE_COMMENT);
    }

    @Operation(summary = "댓글 수정", description = "특정 게시판의 BOARD_TTL 만료 시간 안의 댓글을 수정합니다.")
    @PatchMapping()
    public ResponseEntity<String> updateCommentsByMembersAndCommentId(
            @AuthenticationPrincipal(expression = "members") Members members
            , @RequestBody UpdateCommentsDto updateCommentsDto) {
        Util.checkMemberExist(members);
        commentsService.updateCommentsByMembersAndCommentId(members, updateCommentsDto);
        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_UPDATE_COMMENT);
    }
}
