/*
 * 클래스 설명 : SSE에 대한 로직을 정의한 클래스
 * 메소드 설명
 * - createEmitter : SSE 연결을 생성하는 메소드
 * - deleteEmitter : SSE 연결을 삭제하는 메소드
 * - sendKeywordList : clientId에 대한 SSE 연결이 존재하면 keywordList를 전송하는 메소드
 * - send : SSE에서 데이터를 전송할 때 사용되는 메소드
 */
package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.RealTimeBoardKeyExpiredEvent;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.board.repository.SseEmitterRepository;
import java.io.IOException;
import java.util.Set;
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
    private static final String REALTIME_BOARD_EXPIRED_EMITTER_NAME = "realtimeBoardExpired";

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
        Top10WithChange top10WithChange = event.getTop10WithChange();
        String clientId = event.getClientId();

        sseEmitterRepository.findById(clientId)
                .ifPresent(sseEmitter -> send(top10WithChange, SIGNAL_KEYWORD_LIST_EMITTER_NAME,
                        clientId, sseEmitter));
    }

    /**
     * 연결된 모든 SSE에게 실시간 게시판 만료 이벤트를 보내는 메서드
     */
    public void sendRealTimeBoardExpired(RealTimeBoardKeyExpiredEvent event) {
        Set<String> allClientId = sseEmitterRepository.findAllClientId();
        if (allClientId.isEmpty()) {
            log.info("연결된 SSE가 없으므로 실시간 게시판 만료를 보낼 곳이 없습니다.");
            return;
        }

        for (String clientId : allClientId) {
            sseEmitterRepository.findById(clientId)
                    .ifPresent(sseEmitter -> send(event, REALTIME_BOARD_EXPIRED_EMITTER_NAME,
                            clientId, sseEmitter));
        }
    }

    public void send(Object data, String emitterName, String clientId, SseEmitter sseEmitter) {
        try {
            log.info("SSE에서 전송한 clientId: {},데이터 :[{}]", clientId, data);
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
