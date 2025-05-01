package com.trend_now.backend.comment.presentation;

import com.trend_now.backend.comment.application.CommentsService;
import com.trend_now.backend.comment.data.vo.DeleteComments;
import com.trend_now.backend.comment.data.vo.FindAllComments;
import com.trend_now.backend.comment.data.vo.SaveComments;
import com.trend_now.backend.comment.data.vo.UpdateComments;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.common.Util;
import com.trend_now.backend.member.domain.Members;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
@Tag(name = "Comment API", description = "댓글 관련 API")
public class CommentsController {

    private static final String SUCCESS_SAVE_COMMENT = "댓글 작성에 성공했습니다.";
    private static final String SUCCESS_DELETE_COMMENT = "댓글 삭제에 성공했습니다.";
    private static final String SUCCESS_UPDATE_COMMENT = "댓글 수정에 성공했습니다.";

    private final CommentsService commentsService;
    private final CommentsRepository commentsRepository;

    @Operation(summary = "댓글 저장", description = "게시글에 댓글을 작성합니다.")
    @PostMapping()
    public ResponseEntity<String> saveComments(@AuthenticationPrincipal(expression = "members") Members member
            , @RequestBody SaveComments saveComments) {
        Util.checkMemberExist(member);
        commentsService.saveComments(member, saveComments);
        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_SAVE_COMMENT);
    }

    @Operation(summary = "댓글 조회", description = "게시글에 댓글을 조회합니다.")
    @GetMapping("/{postId}")
    public ResponseEntity<List<FindAllComments>> findAllCommentsByPostId(@PathVariable Long postId) {
        return ResponseEntity.status(HttpStatus.OK).body(commentsRepository.findByPostsIdOrderByCreatedAtDesc(postId));
    }

    @Operation(summary = "댓글 삭제", description = "특정 게시판의 BOARD_TTL 만료 시간 안의 댓글을 삭제합니다.")
    @DeleteMapping()
    public ResponseEntity<String> deleteCommentsByMembersAndCommentId(
            @AuthenticationPrincipal(expression = "members") Members member
            , @RequestBody DeleteComments deleteComments) {
        Util.checkMemberExist(member);
        commentsService.deleteCommentsByCommentId(member, deleteComments);
        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_DELETE_COMMENT);
    }

    @Operation(summary = "댓글 수정", description = "특정 게시판의 BOARD_TTL 만료 시간 안의 댓글을 수정합니다.")
    @PatchMapping()
    public ResponseEntity<String> updateCommentsByMembersAndCommentId(
            @AuthenticationPrincipal(expression = "members") Members members
            , @RequestBody UpdateComments updateComments) {
        Util.checkMemberExist(members);
        commentsService.updateCommentsByMembersAndCommentId(members, updateComments);
        return ResponseEntity.status(HttpStatus.OK).body(SUCCESS_UPDATE_COMMENT);
    }
}
