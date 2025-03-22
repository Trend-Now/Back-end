package com.trend_now.backend.board.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalKeywordService {

    private static final String SIGNAL_BZ_BASE_URI = "/news/realtime";
    private static final String CLIENT_ERROR_MESSAGE = "4xx";
    private static final String SERVER_ERROR_MESSAGE = "5xx";
    private static final String JSON_PARSE_ERROR_MESSAGE = "JSON 파싱에 오류가 생겼습니다.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public Mono<SignalKeywordDto> fetchRealTimeKeyword() {
        return webClient.get()
                .uri(SIGNAL_BZ_BASE_URI)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    throw new RuntimeException(CLIENT_ERROR_MESSAGE);
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    throw new RuntimeException(SERVER_ERROR_MESSAGE);
                })
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        SignalKeywordDto signalKeywordDto = objectMapper.readValue(response,
                                SignalKeywordDto.class);
                        return Mono.just(signalKeywordDto);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException(JSON_PARSE_ERROR_MESSAGE));
                    }
                });
    }
}
