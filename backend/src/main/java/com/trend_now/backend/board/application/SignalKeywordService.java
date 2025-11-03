package com.trend_now.backend.board.application;

import static com.trend_now.backend.board.application.BoardRedisService.BOARD_RANK_KEY;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.dto.BoardKeyProvider;
import com.trend_now.backend.board.dto.MsgFormat;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.board.dto.Top10WithDiff;
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
    private static final String NOT_FOUND_PREVIOUS_KEYWORD_ERROR_MESSAGE = "기존에 저장 돼 있던 랭킹 정보를 찾을 수 없습니다.";
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

    // 실시간 검색어가 마지막으로 갱신된 시간 업데이트
    public void updateLastUpdatedTime(long timestamp) {
        redisTemplate.opsForValue()
            .set(REALTIME_KEYWORD_LAST_UPDATED_KEY, String.valueOf(timestamp));
    }

    public void addNewRealtimeKeyword(BoardKeyProvider boardKeyProvider, int currRank) {
        // 새로 들어온 키워드를 realtime_keywords에 추가
        Top10WithDiff top10WithDiff = new Top10WithDiff(currRank, boardKeyProvider.getBoardName(),
            boardKeyProvider.getBoardId(), RankChangeType.NEW, 0);
        redisTemplate.opsForList()
            .rightPush(SIGNAL_KEYWORD_LIST, top10WithDiff.toRealtimeKeywordsKey());
    }

    public void addRealtimeKeywordWithRankTracking(Long boardId, String oldBoardName, String newBoardName, int currRank) {
        String key = oldBoardName + ":" + boardId;

        Long preRank = redisTemplate.opsForZSet().rank(BOARD_RANK_KEY, key);
        if (preRank == null) {
            throw new RuntimeException(NOT_FOUND_PREVIOUS_KEYWORD_ERROR_MESSAGE);
        }
        // currRank는 1부터 시작, preRank는 0부터 시작하므로 preRank에 1을 더해서 계산
        long diffRank = (preRank + 1) - currRank;
        RankChangeType rankChangeType = diffRank == 0 ? RankChangeType.SAME
            : diffRank < 0 ? RankChangeType.DOWN : RankChangeType.UP;

        Top10WithDiff top10WithDiff = new Top10WithDiff(currRank, newBoardName,
            boardId, rankChangeType, Math.abs((int) diffRank));
        redisTemplate.opsForList()
            .rightPush(SIGNAL_KEYWORD_LIST, top10WithDiff.toRealtimeKeywordsKey());
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

    public void deleteOldRealtimeKeywords() {
        redisTemplate.opsForList().trim(SIGNAL_KEYWORD_LIST, -10, -1);
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
