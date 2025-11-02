package com.trend_now.backend.board.cache;

import static com.trend_now.backend.board.application.BoardRedisService.BOARD_KEY_DELIMITER;
import static com.trend_now.backend.board.application.BoardRedisService.BOARD_RANK_KEY;
import static com.trend_now.backend.board.application.BoardRedisService.KEY_LIVE_TIME;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.trend_now.backend.board.application.BoardKeyProvider;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.search.dto.BoardRedisKey;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardCache {

    public static final long EXPIRATION_TIME = KEY_LIVE_TIME + 5; // 캐시 만료 시간 (랭크 변화 추이 계산을 위해 5초의 여유 시간 추가)
    public static final int MAXIMUM_SIZE = 1000; // 캐시 최대 크기
    public static final double SIMILARITY_THRESHOLD = 0.3; // 유사도 임계값 (0.3 = 두 단어 이상 일치)
    public static final String BLANK = "\\s+"; // 공백 정규식
    public static final String SPECIAL_CHAR_PATTERN = "[^가-힣a-zA-Z0-9\\s]";


    private final BoardRepository boardRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Getter
    private final Cache<Long, BoardCacheEntry> boardCacheEntryMap = Caffeine.newBuilder()
        // write 작업이 일어난 이후 30분 뒤 캐시 만료
        .expireAfterWrite(EXPIRATION_TIME, TimeUnit.SECONDS)
        // 캐시의 최대 크기 설정
        .maximumSize(MAXIMUM_SIZE)
        .build();

    @Getter
    private final Cache<Long, BoardCacheEntry> fixedBoardCacheMap = Caffeine.newBuilder()
        // 캐시의 최대 크기 설정
        .maximumSize(MAXIMUM_SIZE)
        .build();

    public void addRealtimeBoardCache(BoardKeyProvider boardKeyProvider) {
        boardCacheEntryMap.put(boardKeyProvider.getBoardId(), BoardCacheEntry.builder()
            .boardName(boardKeyProvider.getBoardName())
            .splitBoardNameByBlank(Arrays.stream(
                    boardKeyProvider.getBoardName().replaceAll(SPECIAL_CHAR_PATTERN, "").split(BLANK))
                .collect(Collectors.toSet()))
            .build());
    }

    @Async
    public void setBoardInfo(Set<String> boardRank) {
        // 실시간 게시판 캐시 초기화
        boardCacheEntryMap.invalidateAll();

        // 캐시 생성
        List<Long> boardCacheIdList = boardRank.stream().map(
            keyword -> Long.parseLong(keyword.split(":")[1])
        ).toList();
        List<Boards> boardsList = boardRepository.findByIdIn(boardCacheIdList);
        boardsList.forEach(boards ->
            boardCacheEntryMap.put(boards.getId(), BoardCacheEntry.builder()
                .boardName(boards.getName())
                .splitBoardNameByBlank(Arrays.stream(
                        boards.getName().replaceAll(SPECIAL_CHAR_PATTERN, "").split(BLANK))
                    .collect(Collectors.toSet()))
                .createdAt(boards.getCreatedAt())
                .updatedAt(boards.getUpdatedAt())
                .build())
        );
    }

    @PostConstruct
    public void init() {
        initRealtimeBoard();
        initFixedBoard();
    }

    // 실시간 게시판 초기화
    public void initRealtimeBoard() {
        // 실시간 게시판 캐시 초기화
        boardCacheEntryMap.invalidateAll();

        // 만약 redis에 저장된 실시간 게시판 순위가 없다면 캐싱 작업 중단
        Set<String> boardRankList = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
        if (boardRankList == null || boardRankList.isEmpty()) {
            return;
        }

        // 캐시 생성
        boardRankList.forEach(
            boardRankValue -> {
                String[] split = boardRankValue.split(BOARD_KEY_DELIMITER);
                String boardKeyword = split[0];
                long boardId = Long.parseLong(split[1]);
                boardCacheEntryMap.put(boardId,
                    BoardCacheEntry.builder()
                        .boardName(boardKeyword)
                        .splitBoardNameByBlank(
                            Arrays.stream(boardKeyword.split(BLANK))
                                .collect(Collectors.toSet()))
                        .build()
                );
            }
        );
    }

    // 고정 게시판 초기화
    public void initFixedBoard() {
        // 고정 게시판 캐시 초기화
        fixedBoardCacheMap.invalidateAll();

        // 캐시 생성
        List<Boards> fixedBoardList = boardRepository.findByBoardCategory(BoardCategory.FIXED);
        fixedBoardList.forEach(
            fixedBoard -> fixedBoardCacheMap.put(fixedBoard.getId(),
                BoardCacheEntry.builder()
                    .boardName(fixedBoard.getName())
                    .build()
            )
        );
    }

    public boolean isInBoardCache(Long boardId) {
        return boardCacheEntryMap.getIfPresent(boardId) != null;
    }

    /**
     * 키워드와 유사한 게시판 이름이 존재하는지 확인하고, 유사한 게시판이 있으면 해당 이름을 반환. 유사한 게시판이 없으면 입력 값으로 들어온 키워드를 그대로 반환.
     */
    public BoardRedisKey findKeywordSimilarity(String newKeyword) {
        Set<String> newSet = Arrays.stream(newKeyword
                .replaceAll(SPECIAL_CHAR_PATTERN, "")
                .split(BLANK)
            ).collect(Collectors.toSet());

        ConcurrentMap<Long, BoardCacheEntry> cacheMap = boardCacheEntryMap.asMap();
        for (Long boardId : cacheMap.keySet()) {
            BoardCacheEntry boardCacheEntry = cacheMap.get(boardId);
            Set<String> originSet = boardCacheEntry.getSplitBoardNameByBlank();
            // 교집합
            Set<String> intersection = new HashSet<>(newSet);
            intersection.retainAll(originSet);

            // 합집합
            Set<String> union = new HashSet<>(newSet);
            union.addAll(originSet);

            double similarity = (double) intersection.size() / union.size();

            if (similarity < 1.0 && similarity > SIMILARITY_THRESHOLD) {
                log.info("유사한 게시판 발견 - 새로운 키워드: {}, 기존 키워드: {}, 유사도: {}", newKeyword,
                    boardCacheEntry.getBoardName(), similarity);
                return new BoardRedisKey(boardId, boardCacheEntry.getBoardName());
            }
        }

        return new BoardRedisKey(null, newKeyword);
    }
}
