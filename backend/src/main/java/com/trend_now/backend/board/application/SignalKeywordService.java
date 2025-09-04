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
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String SIGNAL_KEYWORD_LIST_EMITTER_NAME = "signalKeywordList";
    private static final String SIGNAL_KEYWORD_LIST = "realtime_keywords";

    private final WebClient.Builder webClientBuilder;
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
     * 실시간 검색어 순위 변동 추이를 계산하는 함수이다 Redis에 실시간 검색어 순위가 저장되어 있지 않으면 서버가 처음 시작되었음을 의미하고, 현재 순위를 저장하고 모두
     * NEW로 처리 이전 순위가 있으면 비교하여 변동 추이를 계산한다
     */
    public Top10WithChange calculateRankChange(SignalKeywordDto signalKeywordDto) {
        long now = signalKeywordDto.getNow();
        List<Top10> currentKeywordList = signalKeywordDto.getTop10();
        List<Top10WithDiff> keywordDiffList = new ArrayList<>();

        // 이전의 검색어 순위를 가져와 저장
        List<String> previousKeywordList = redisTemplate.opsForList()
                .range(SIGNAL_KEYWORD_LIST, 0, -1);

        Map<String, Integer> previousRankMap = new HashMap<>();
        if (previousKeywordList != null) {
            for (int i = 0; i < previousKeywordList.size(); i++) {
                previousRankMap.put(previousKeywordList.get(i), i + 1);
            }
        }

        for (Top10 currentKeyword : currentKeywordList) {
            String keyword = currentKeyword.getKeyword();
            int currentRank = currentKeyword.getRank();

            if (previousRankMap.containsKey(keyword)) {
                /* 이전의 검색어 순위에 현재의 검색어가 존재할 경우 비교를 진행 */
                Integer previousRank = previousRankMap.get(keyword);
                RankChangeType changeType;

                if (previousRank != null) {
                    if (currentRank < previousRank) {
                        // 이전 순위보다 상승한 경우
                        changeType = RankChangeType.UP;
                    } else if (currentRank > previousRank) {
                        // 이전 순위보다 하락한 경우
                        changeType = RankChangeType.DOWN;
                    } else {
                        // 이전 순위와 동일한 경우
                        changeType = RankChangeType.SAME;
                    }
                } else {
                    changeType = RankChangeType.NEW;
                }

                keywordDiffList.add(
                        new Top10WithDiff(currentRank, keyword, changeType, previousRank));
            } else {
                /* 이전의 검색어 순위가 존재하지 않기 때문에 모두 NEW로 처리 */
                keywordDiffList.add(new Top10WithDiff(currentRank, keyword, RankChangeType.NEW, 0));
            }
        }

        redisTemplate.delete(SIGNAL_KEYWORD_LIST);
        currentKeywordList.forEach(keyword ->
                redisTemplate.opsForList().rightPush(SIGNAL_KEYWORD_LIST, keyword.getKeyword())
        );

        return new Top10WithChange(now, keywordDiffList);
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
