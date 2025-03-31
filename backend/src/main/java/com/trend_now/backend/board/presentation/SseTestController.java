/*
 * 클래스 설명 : SSE 테스트를 위한 컨트롤러
 * 메소드 설명
 * - sseTest() : SSE 테스트 타임리프 페이지
 */
package com.trend_now.backend.board.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SseTestController {

    @GetMapping("/sse-test")
    public String sseTest() {
        return "sse-test";
    }
}
