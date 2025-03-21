package com.trend_now.backend.board.presentation;

import com.trend_now.backend.board.application.SignalKeywordService;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SignalKeywordController {

    private static final String SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE = "실시간 검색어 컨트롤러가 호출되었습니다.";

    private final SignalKeywordService signalKeywordService;

    @GetMapping("/news/realtime")
    public Mono<SignalKeywordDto> getRealTimeNews() {
        log.info(SIGNAL_BZ_KEYWORD_SUCCESS_MESSAGE);
        return signalKeywordService.fetchRealTimeKeyword();
    }
}
