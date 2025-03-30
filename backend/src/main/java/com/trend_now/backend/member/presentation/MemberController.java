package com.trend_now.backend.member.presentation;

import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.application.GoogleService;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.data.vo.AuthCodeToJwtRequest;
import com.trend_now.backend.member.data.vo.GoogleLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@Tag(name = "회원 서비스", description = "회원 API")
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/member")
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleService googleService;

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
    @PostMapping("/google/login")
    public ResponseEntity<GoogleLoginResponse> googleLogin(@RequestBody AuthCodeToJwtRequest authCodeToJwtRequest) {
        return new ResponseEntity<>(googleService.getToken(authCodeToJwtRequest), HttpStatus.OK);
    }
}
