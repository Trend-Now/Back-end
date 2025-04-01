package com.trend_now.backend.board.repository;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter save(String eventId, SseEmitter sseEmitter) {
        emitters.put(eventId, sseEmitter);
        return sseEmitter;
    }

    public Optional<SseEmitter> findById(String memberId) {
        return Optional.ofNullable(emitters.get(memberId));
    }

    public void deleteById(String eventId) {
        emitters.remove(eventId);
    }

    /**
     * 연결된 모든 SSE의 clientId를 불변 객체로 반환한다
     * 불변 객체로 반환하기 때문에 외부 클래스에서 emitters에 대해서 직접 수정은 불가능하다
     */
    public Set<String> findAllClientId() {
        return Collections.unmodifiableSet(emitters.keySet());
    }
}
