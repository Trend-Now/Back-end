package com.trend_now.backend.board.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.dto.MsgFormat;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.board.dto.Top10WithDiff;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalKeywordService {

    private static final String SIGNAL_BZ_BASE_URL = "https://api.signal.bz";
    private static final String SIGNAL_BZ_REQUEST_URI = "/news/realtime";
    private static final String CLIENT_ERROR_MESSAGE = "4xx";
    private static final String SERVER_ERROR_MESSAGE = "5xx";
    private static final String JSON_PARSE_ERROR_MESSAGE = "JSON 파싱에 오류가 생겼습니다.";
    private static final String FETCH_KEYWORD_ERROR_MESSAGE = "실시간 검색어 순위 리스트가 존재하지 않습니다.";
    private static final String REALTIME_KEYWORD_LAST_UPDATED_KEY = "realtime_keywords:last_updated";
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String SUBSCRIPTION_SUCCESS_EMITTER_NAME = "subscriptionSuccess";
    public static final String SIGNAL_KEYWORD_LIST = "realtime_keywords";

    private final Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final SseEmitterService sseEmitterService;

    public Mono<SignalKeywordDto> fetchRealTimeKeyword() {
        WebClient webClient = webClientBuilder.baseUrl(SIGNAL_BZ_BASE_URL).build();
        return webClient.get()
            .uri(SIGNAL_BZ_REQUEST_URI)
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

    /**
     * 순위 변동 추이를 계산하여 Redis - realtime_keywords에 저장하는 함수 기존에 저장돼 있던 값은 전부 삭제하고, 새로 받아온 값으로 갱신한다.
     */
    public List<String> saveRealtimeKeywords(SignalKeywordDto signalKeywordDto,
        List<Long> boardIdList) {
        // 이 메서드에서 저장한 값을 반환하기 위한 리스트
        List<String> realTimeKeywordList = new ArrayList<>();
        // signal.bz에서 받아온 현재 실시간 검색어 리스트
        List<Top10> currentKeywordList = signalKeywordDto.getTop10();
        // 이전에 저장돼 있던 값을 Map 형태로 변환하여 조회
        Map<String, Integer> previousRankMap = getRealtimeKeywordMap();
        for (int i = 0; i < currentKeywordList.size(); i++) {
            Top10 currentKeyword = currentKeywordList.get(i);
            // 현재 순위
            Integer currentRank = currentKeyword.getRank();
            // 키워드 (게시판 이름)
            String keyword = currentKeyword.getKeyword();
            // 게시판 아이디
            Long boardId = boardIdList.get(i);
            // 이전에 저장돼 있던 순위
            Integer previousRank = previousRankMap.get(currentKeyword.getKeyword());
            // signal.bz에서 제공하는 변동 상태 (만약 TrendNow 시스템이 처음 시작돼서 이전에 저장돼 있던 값이 없다면 NEW로 처리)
            RankChangeType state = previousRank == null ? RankChangeType.NEW
                : currentRank.equals(previousRank) ? RankChangeType.SAME
                    : currentRank < previousRank ? RankChangeType.UP
                        : RankChangeType.DOWN;
            // 변동폭
            // state 값이 NEW/SAME 이거나 이전에 저장 돼 있던 값이 없다면 0
            // state 값이 UP/DOWN 이면서 이전에 저장 돼 있던 값이 있다면 절대값으로 계산)
            Integer diffRank =
                state == RankChangeType.NEW || state == RankChangeType.SAME
                    ? 0
                    : Math.abs(currentRank - previousRank);

            String value = String.format("%s:%s:%s:%s:%s", currentRank, keyword, boardId, state,
                diffRank);
            realTimeKeywordList.add(value);
            redisTemplate.opsForList().rightPush(SIGNAL_KEYWORD_LIST, value);
        }
        // 이전에 저장 돼 있던 값은 전부 삭제
        redisTemplate.opsForList().trim(SIGNAL_KEYWORD_LIST, -currentKeywordList.size(), -1);
        return realTimeKeywordList;
    }

    // 실시간 검색어가 마지막으로 갱신된 시간 업데이트
    public void updateLastUpdatedTime(long timestamp) {
        redisTemplate.opsForValue()
            .set(REALTIME_KEYWORD_LAST_UPDATED_KEY, String.valueOf(timestamp));
    }

    // 실시간 검색어가 마지막으로 갱신된 시간 조회
    public long getLastUpdatedTime() {
        String lastUpdated = redisTemplate.opsForValue().get(REALTIME_KEYWORD_LAST_UPDATED_KEY);
        if (lastUpdated == null) {
            return 0L;
        }
        return Long.parseLong(lastUpdated);
    }

    public Top10WithChange getRealTimeKeyword() {
        List<String> realtimeKeywordList = redisTemplate.opsForList()
            .range(SIGNAL_KEYWORD_LIST, 0, -1);
        if (realtimeKeywordList == null || realtimeKeywordList.isEmpty()) {
            throw new RuntimeException(FETCH_KEYWORD_ERROR_MESSAGE);
        }
        long lastUpdatedTime = getLastUpdatedTime();
        List<Top10WithDiff> top10WithDiffList = realtimeKeywordList.stream()
            .map(Top10WithDiff::from).toList();
        return new Top10WithChange(lastUpdatedTime, top10WithDiffList);
    }

    private Map<String, Integer> getRealtimeKeywordMap() {
        List<String> previousKeywordList = redisTemplate.opsForList()
            .range(SIGNAL_KEYWORD_LIST, 0, -1);
        Map<String, Integer> previousRankMap = new HashMap<>();
        if (previousKeywordList != null) {
            for (int i = 0; i < previousKeywordList.size(); i++) {
                String keyword = previousKeywordList.get(i).split(":")[1];
                previousRankMap.put(keyword, i + 1);
            }
        }
        return previousRankMap;
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
            redisTemplate.opsForSet().remove(CLIENT_ID_KEY, clientId);
        });

        sseEmitterService.send(MsgFormat.SUBSCRIBE, SUBSCRIPTION_SUCCESS_EMITTER_NAME, clientId,
            sseEmitter);
        return sseEmitter;
    }
}
