package com.trend_now.backend.member.presentation;

import com.trend_now.backend.comment.application.CommentsService;
import com.trend_now.backend.comment.data.dto.CommentInfoDto;
import com.trend_now.backend.comment.data.dto.CommentListPagingResponseDto;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.data.dto.MyPageResponseDto;
import com.trend_now.backend.member.data.dto.RefreshTokenRequestDto;
import com.trend_now.backend.member.data.dto.UpdateNicknameRequestDto;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.application.ScrapService;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.dto.MyPostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@Tag(name = "회원 서비스", description = "회원 API")
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/member")
public class MemberController {

    private final MemberService memberService;
    private final ScrapService scrapService;
    private final PostsService postsService;
    private final CommentsService commentsService;

    private static final String NICKNAME_UPDATE_SUCCESS_MESSAGE = "닉네임 변경 완료";
    private static final String WITHDRAWAL_SUCCESS_MESSAGE = "회원 탈퇴가 완료";
    private static final String FIND_SCRAP_POSTS_SUCCESS_MESSAGE = "사용자가 스크랩한 게시글 조회 완료";
    private static final String FIND_MEMBER_POSTS_SUCCESS_MESSAGE = "사용자가 작성한 게시글 조회 완료";
    private static final String FIND_MEMBER_COMMENTS_SUCCESS_MESSAGE = "사용자가 작성한 댓글 조회 완료";
    private static final String REISSUANCE_ACCESS_TOKEN_SUCCESS = "Access Token 재발급에 성공하였습니다.";


    // 연결 확인
    @GetMapping("")
    @Operation(summary = "연결 확인", description = "연결 확인 API")
    public ResponseEntity<String> connectionCheck() {
        return new ResponseEntity<>("Connection Success", HttpStatus.OK);
    }

    // 테스트용 JWT 발급 API
    @GetMapping("/test-jwt")
    @Operation(summary = "JWT 발급", description = "테스트용 JWT 발급 API")
    public ResponseEntity<String> getJwt(HttpServletRequest request, HttpServletResponse response) {
        return new ResponseEntity<>(memberService.getTestJwt(request, response), HttpStatus.OK);
    }

    /**
     * 마이페이지 조회 API
     */
    @GetMapping("/me")
    @Operation(summary = "마이페이지 조회", description = "회원의 마이페이지 정보를 조회합니다.")
    public ResponseEntity<MyPageResponseDto> getMyPage(
        @AuthenticationPrincipal(expression = "members") Members member) {
        MyPageResponseDto myPageResponseDto = memberService.getMyPage(member.getId());
        return new ResponseEntity<>(myPageResponseDto, HttpStatus.OK);
    }

    /**
     * 닉네임 변경 API
     */
    @PatchMapping("/nickname")
    @Operation(summary = "닉네임 변경", description = "닉네임 변경을 요청한 사용자의 닉네임을 변경합니다.")
    public ResponseEntity<String> updateNickname(
        @AuthenticationPrincipal(expression = "members") Members member,
        @RequestBody @Valid UpdateNicknameRequestDto nicknameRequest) {
        memberService.updateNickname(member, nicknameRequest.nickname());
        return new ResponseEntity<>(NICKNAME_UPDATE_SUCCESS_MESSAGE, HttpStatus.OK);
    }

    /**
     * <pre>
     * 회원 탈퇴 API
     * API가 호출되면 해당 회원의 값을 DB에서 물리적으로 삭제함
     * </pre>
     */
    @DeleteMapping("/withdrawal")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 요청한 사용자의 정보를 삭제합니다. 해당 사용자가 작성한 글의 작성자는 NULL로 변경됩니다.")
    public ResponseEntity<String> withdrawMember(
        @AuthenticationPrincipal(expression = "members") Members member) {
        memberService.deleteMember(member.getId());
        return new ResponseEntity<>(WITHDRAWAL_SUCCESS_MESSAGE, HttpStatus.NO_CONTENT);
    }

    /**
     * 회원이 스크랩한 게시글 조회 API
     */
    @GetMapping("/scrap")
    @Operation(summary = "스크랩한 게시글 목록 조회", description = "회원이 스크랩한 게시글을 조회합니다.")
    public ResponseEntity<MyPostListResponse> getMemberScrapPosts(
        @AuthenticationPrincipal(expression = "members") Members member,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {

        Page<PostWithBoardSummaryDto> scrappedPostsByMemberId = scrapService.getScrappedPostsByMemberId(
            member.getId(), page - 1, size);
        return new ResponseEntity<>(
            MyPostListResponse.of(FIND_SCRAP_POSTS_SUCCESS_MESSAGE,
                scrappedPostsByMemberId.getTotalPages(), scrappedPostsByMemberId.getTotalElements(), scrappedPostsByMemberId.getContent()),
            HttpStatus.OK);
    }

    /**
     * 회원이 작성한 게시글 조회 API
     */
    @GetMapping("/posts")
    @Operation(summary = "회원이 작성한 게시글 목록 조회", description = "회원이 작성한 게시글을 조회합니다.")
    public ResponseEntity<MyPostListResponse> getMemberPosts(
        @AuthenticationPrincipal(expression = "members") Members member,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        Page<PostWithBoardSummaryDto> postsByMemberId = postsService.getPostsByMemberId(member.getId(), page - 1,
            size);

        return new ResponseEntity<>(MyPostListResponse.of(FIND_MEMBER_POSTS_SUCCESS_MESSAGE,
            postsByMemberId.getTotalPages(), postsByMemberId.getTotalElements(), postsByMemberId.getContent()), HttpStatus.OK);
    }

    @GetMapping("/comments")
    @Operation(summary = "회원이 작성한 댓글 목록 조회", description = "회원이 작성한 댓글을 조회합니다.")
    public ResponseEntity<CommentListPagingResponseDto> getMemberComments(
        @AuthenticationPrincipal(expression = "members") Members member,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        Page<CommentInfoDto> commentsByMemberId = commentsService.getCommentsByMemberId(
            member.getId(), page - 1, size);
        return new ResponseEntity<>(
            CommentListPagingResponseDto.of(FIND_MEMBER_COMMENTS_SUCCESS_MESSAGE,
                commentsByMemberId.getTotalPages(), commentsByMemberId.getTotalElements(), commentsByMemberId.getContent()),
            HttpStatus.OK);
    }

    @PostMapping("/access-token")
    @Operation(summary = "Access Token 재발급 API", description = "Access Token을 재발급합니다.")
    public ResponseEntity<String> reissuanceAccessToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        return new ResponseEntity<>(memberService.reissuanceAccessToken(request, response), HttpStatus.OK);
    }
}
