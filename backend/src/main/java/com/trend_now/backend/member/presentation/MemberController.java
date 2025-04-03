package com.trend_now.backend.member.presentation;

import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.application.GoogleService;
import com.trend_now.backend.member.application.KakaoService;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.application.NaverService;
import com.trend_now.backend.member.data.dto.UpdateNicknameRequestDto;
import com.trend_now.backend.member.data.vo.AuthCodeToJwtRequest;
import com.trend_now.backend.member.data.vo.OAuth2LoginResponse;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleService googleService;
    private final KakaoService kakaoService;
    private final NaverService naverService;

    // 연결 확인
    @GetMapping("")
    @Operation(summary = "연결 확인", description = "연결 확인 API")
    public ResponseEntity<String> connectionCheck() {
        return new ResponseEntity<>("Connection Success", HttpStatus.OK);
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
     *  닉네임 변경 API
     */
    @PatchMapping("/nickname")
    public ResponseEntity<String> updateNickname(@AuthenticationPrincipal Members member, @RequestBody UpdateNicknameRequestDto nicknameRequest) {
        memberService.updateNickname(member, nicknameRequest.nickname());
        return new ResponseEntity<>("닉네임 변경이 완료 되었습니다.", HttpStatus.OK);
    }

    /**
     * 회원 탈퇴 API
     * API가 호출되면 해당 회원의 값을 DB에서 물리적으로 삭제함
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMember(@AuthenticationPrincipal Members member) {
        memberService.deleteMember(member);
        return new ResponseEntity<>("회원 탈퇴가 완료 되었습니다.", HttpStatus.NO_CONTENT);
    }
}
