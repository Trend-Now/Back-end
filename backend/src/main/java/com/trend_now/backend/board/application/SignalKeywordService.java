package com.trend_now.backend.board.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.dto.MsgFormat;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalKeywordService {

    private static final String SIGNAL_BZ_BASE_URI = "/news/realtime";
    private static final String CLIENT_ERROR_MESSAGE = "4xx";
    private static final String SERVER_ERROR_MESSAGE = "5xx";
    private static final String JSON_PARSE_ERROR_MESSAGE = "JSON 파싱에 오류가 생겼습니다.";
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String SIGNAL_KEYWORD_LIST_EMITTER_NAME = "signalKeywordList";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final SseEmitterService sseEmitterService;

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

    private void saveClientId(String clientId) {
        redisTemplate.opsForSet().add(CLIENT_ID_KEY, clientId);
    }

    public void deleteClientId(String clientId) {
        redisTemplate.opsForSet().remove(CLIENT_ID_KEY, clientId);
    }

    public Set<String> findAllClientId() {
        return redisTemplate.opsForSet().members(CLIENT_ID_KEY);
    }

    public SseEmitter subscribe(String clientId) {
        saveClientId(clientId);
        SseEmitter sseEmitter = sseEmitterService.createEmitter(clientId);

        sseEmitter.onTimeout(sseEmitter::complete);
        sseEmitter.onError((e) -> sseEmitter.complete());
        sseEmitter.onCompletion(() -> {
            sseEmitterService.deleteEmitter(clientId);
        });

        sseEmitterService.send(MsgFormat.SUBSCRIBE, SIGNAL_KEYWORD_LIST_EMITTER_NAME, clientId,
                sseEmitter);
        return sseEmitter;
    }
}
