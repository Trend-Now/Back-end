package com.trend_now.backend.board.cache;

import static com.trend_now.backend.board.application.SignalKeywordService.SIGNAL_KEYWORD_LIST;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.Top10WithDiff;
import com.trend_now.backend.board.repository.BoardRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardCache {

    public static final int EXPIRATION_TIME = 30; // 캐시 만료 시간 (분 단위)
    public static final int MAXIMUM_SIZE = 1000; // 캐시 최대 크기

    private final BoardRepository boardRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Getter
    private final Cache<Long, BoardCacheEntry> boardCacheEntryMap = Caffeine.newBuilder()
        // write 작업이 일어난 이후 30분 뒤 캐시 만료
        .expireAfterWrite(EXPIRATION_TIME, TimeUnit.MINUTES)
        // 캐시의 최대 크기 설정
        .maximumSize(MAXIMUM_SIZE)
        .build();

    @Getter
    private final Cache<Long, BoardCacheEntry> fixedBoardCacheMap = Caffeine.newBuilder()
        // 캐시의 최대 크기 설정
        .maximumSize(MAXIMUM_SIZE)
        .build();

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
        List<String> boardRankList = redisTemplate.opsForList().range(SIGNAL_KEYWORD_LIST, 0, -1);
        if (boardRankList == null || boardRankList.isEmpty()) {
            return;
        }

        // 캐시 생성
        boardRankList.forEach(
            boardRankValue -> {
                Top10WithDiff boardRankInfo = Top10WithDiff.from(boardRankValue);
                boardCacheEntryMap.put(boardRankInfo.getBoardId(),
                    BoardCacheEntry.builder()
                        .boardName(boardRankInfo.getKeyword())
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
}
