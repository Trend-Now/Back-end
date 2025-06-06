package com.trend_now.backend.member.presentation;

import com.trend_now.backend.member.application.GoogleService;
import com.trend_now.backend.member.application.KakaoService;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.application.NaverService;
import com.trend_now.backend.member.data.dto.UpdateNicknameRequestDto;
import com.trend_now.backend.member.data.vo.AuthCodeToJwtRequest;
import com.trend_now.backend.member.data.vo.OAuth2LoginResponse;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.application.ScrapService;
import com.trend_now.backend.post.dto.PostListDto;
import com.trend_now.backend.post.dto.PostListPagingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final GoogleService googleService;
    private final KakaoService kakaoService;
    private final NaverService naverService;
    private final ScrapService scrapService;
    private final PostsService postsService;

    private static final String SUCCESS_GET_JWT = "테스트용 JWT 발급에 성공하였습니다.";
    private static final String NICKNAME_UPDATE_SUCCESS_MESSAGE = "닉네임 변경 완료";
    private static final String WITHDRAWAL_SUCCESS_MESSAGE = "회원 탈퇴가 완료";
    private static final String FIND_SCRAP_POSTS_SUCCESS_MESSAGE = "사용자가 스크랩한 게시글 조회 완료";
    private static final String FIND_MEMBER_POSTS_SUCCESS_MESSAGE = "사용자가 작성한 게시글 조회 완료";

    // 연결 확인
    @GetMapping("")
    @Operation(summary = "연결 확인", description = "연결 확인 API")
    public ResponseEntity<String> connectionCheck() {
        return new ResponseEntity<>("Connection Success", HttpStatus.OK);
    }

    // 테스트용 JWT 발급 API
    @GetMapping("test-jwt")
    @Operation(summary = "JWT 발급", description = "테스트용 JWT 발급 API")
    public ResponseEntity<String> getJwt() {
        return new ResponseEntity<>(memberService.getTestJwt(), HttpStatus.OK);
    }

    /**
     *  구글 인가코드를 받는 Controller
     *  - 프론트엔드에서 구글 인가코드를 가지고 유저 정보를 반환받는 Controller
     *  - 해당 Controller에서 HTTP Body를 통해 인가 코드를 받음
     *  - 해당 인가 코드를 가지고 구글 서버에 사용자 정보 요청
     *  - 사용자 정보를 통해 서비스 유저인지 확인
     *  - 유저인 경우, JWT 토큰 발급
     */
    @PostMapping("/login/google")
    public ResponseEntity<OAuth2LoginResponse> googleLogin(@RequestBody AuthCodeToJwtRequest authCodeToJwtRequest) {
        return new ResponseEntity<>(googleService.getToken(authCodeToJwtRequest), HttpStatus.OK);
    }

    @PostMapping("/login/kakao")
    public ResponseEntity<OAuth2LoginResponse> kakaoLogin(@RequestBody AuthCodeToJwtRequest authCodeToJwtRequest) {
        return new ResponseEntity<>(kakaoService.getToken(authCodeToJwtRequest), HttpStatus.OK);
    }

    @PostMapping("/login/naver")
    public ResponseEntity<OAuth2LoginResponse> naverLogin(@RequestBody AuthCodeToJwtRequest authCodeToJwtRequest) {
        return new ResponseEntity<>(naverService.getToken(authCodeToJwtRequest), HttpStatus.OK);
    }

    /**
     * Postman 테스트용 API
     */
    @PostMapping("/login/test/naver")
    public ResponseEntity<OAuth2LoginResponse> testNaverLogin(@RequestBody AuthCodeToJwtRequest accessToken) {
        return new ResponseEntity<>(naverService.testGetToken(accessToken), HttpStatus.OK);
    }

    /**
     *  닉네임 변경 API
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
     * 회원 탈퇴 API
     * API가 호출되면 해당 회원의 값을 DB에서 물리적으로 삭제함
     */
    @DeleteMapping("/withdrawal")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 요청한 사용자의 정보를 삭제합니다. 해당 사용자가 작성한 글의 작성자는 NULL로 변경됩니다.")
    public ResponseEntity<String> withdrawMember(@AuthenticationPrincipal(expression = "members") Members member) {
        memberService.deleteMember(member.getId());
        return new ResponseEntity<>(WITHDRAWAL_SUCCESS_MESSAGE, HttpStatus.NO_CONTENT);
    }

    /**
     * 회원이 스크랩한 게시글 조회 API
     */
    @GetMapping("/scrap")
    @Operation(summary = "스크랩한 게시글 목록 조회", description = "회원이 스크랩한 게시글을 조회합니다.")
    public ResponseEntity<PostListPagingResponseDto> getMemberScrapPosts(
        @AuthenticationPrincipal(expression = "members") Members member,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

        List<PostListDto> scrappedPostsByMemberId = scrapService.getScrappedPostsByMemberId(
            member.getId(), page, size);
        return new ResponseEntity<>(
            PostListPagingResponseDto.of(FIND_SCRAP_POSTS_SUCCESS_MESSAGE, scrappedPostsByMemberId), HttpStatus.OK);
    }

    /**
     * 회원이 작성한 게시글 조회 API
     */
    @GetMapping("/posts")
    @Operation(summary = "회원이 작성한 게시글 목록 조회", description = "회원이 작성한 게시글을 조회합니다.")
    public ResponseEntity<PostListPagingResponseDto> getMemberPosts(
        @AuthenticationPrincipal(expression = "members") Members member,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        List<PostListDto> postsByMemberId = postsService.getPostsByMemberId(member.getId(), page,
            size);
        return new ResponseEntity<>(
            PostListPagingResponseDto.of(FIND_MEMBER_POSTS_SUCCESS_MESSAGE, postsByMemberId), HttpStatus.OK);
    }
}
