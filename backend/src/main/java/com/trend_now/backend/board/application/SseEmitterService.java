/*
 * 클래스 설명 : SSE에 대한 로직을 정의한 클래스
 * 메소드 설명
 * - createEmitter : SSE 연결을 생성하는 메소드
 * - deleteEmitter : SSE 연결을 삭제하는 메소드
 * - sendKeywordList : clientId에 대한 SSE 연결이 존재하면 keywordList를 전송하는 메소드
 * - send : SSE에서 데이터를 전송할 때 사용되는 메소드
 */
package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import com.trend_now.backend.board.repository.SseEmitterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final String SIGNAL_KEYWORD_LIST_EMITTER_NAME = "signalKeywordList";

    @Value("${sse.timeout}")
    private Long timeout;

    private final SseEmitterRepository sseEmitterRepository;

    public SseEmitter createEmitter(String clientId) {
        return sseEmitterRepository.save(clientId, new SseEmitter(timeout));
    }

    public void deleteEmitter(String clientId) {
        sseEmitterRepository.deleteById(clientId);
    }

    public void sendKeywordList(SignalKeywordEventDto event) {
        SignalKeywordDto signalKeywordDto = event.getSignalKeywordDto();
        String clientId = event.getClientId();

        sseEmitterRepository.findById(clientId)
                .ifPresent(sseEmitter -> send(signalKeywordDto, SIGNAL_KEYWORD_LIST_EMITTER_NAME,
                        clientId, sseEmitter));
    }

    public void send(Object data, String emitterName, String clientId, SseEmitter sseEmitter) {
        try {
            log.info("SSE에서 전송한 clientId: 데이터 {}:[{}]", clientId, data);
            sseEmitter.send(SseEmitter.event()
                    .name(emitterName)
                    .id(clientId)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.error("SSE 전송 중 오류가 발생했습니다.", e);
            sseEmitterRepository.deleteById(clientId);
        }
    }
}
