package com.trend_now.backend.member.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "회원 서비스", description = "회원 API")
@RequestMapping("/api/v1/user")
public class MemberController {

    // 연결 확인
    @GetMapping("")
    @Operation(summary = "연결 확인", description = "연결 확인 API")
    public ResponseEntity<String> connectionCheck() {
        return new ResponseEntity<>("Connection Success", HttpStatus.OK);
    }

}
