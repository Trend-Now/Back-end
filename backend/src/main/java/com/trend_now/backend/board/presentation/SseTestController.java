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
