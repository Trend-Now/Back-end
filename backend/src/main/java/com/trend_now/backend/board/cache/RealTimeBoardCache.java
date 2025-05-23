package com.trend_now.backend.board.cache;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.search.util.SearchKeywordUtil;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealTimeBoardCache {

    private final BoardRepository boardRepository;
    private final SearchKeywordUtil searchKeywordUtil;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<Long, BoardCacheEntry> boardCacheEntryMap = new HashMap<>();
    private final Map<Long, BoardCacheEntry> fixedBoardCacheMap = new HashMap<>();

    @Async
    public void setBoardInfo(Set<String> boardRank) {
        lock.writeLock().lock();
        try {
            List<Long> boardCacheIdList = boardRank.stream().map(
                keyword -> Long.parseLong(keyword.split(":")[1])
            ).toList();
            List<Boards> boardsList = boardRepository.findByIdIn(boardCacheIdList);
            boardCacheEntryMap.clear();
            boardsList.forEach(boards ->
                boardCacheEntryMap.put(boards.getId(), BoardCacheEntry.builder()
                    .boardName(boards.getName())
                    .createdAt(boards.getCreatedAt())
                    .updatedAt(boards.getUpdatedAt())
                    .build())
            );
        // try 구문에서 에러가 발생하더라도 lock은 해제되야 하기 때문에 finally 구문을 사용
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 고정 게시판 초기화
    @PostConstruct
    public void initFixedBoard() {
        lock.writeLock().lock();
        try {
            List<Boards> fixedBoardList = boardRepository.findByNameLikeAndBoardCategory(
                "%", BoardCategory.FIXED);
            fixedBoardList.forEach(
                fixedBoard -> fixedBoardCacheMap.put(fixedBoard.getId(), BoardCacheEntry.builder()
                    .boardName(fixedBoard.getName())
                    .build())
            );
        // try 구문에서 에러가 발생하더라도 lock은 해제되야 하기 때문에 finally 구문을 사용
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<Long, BoardCacheEntry> getBoardCacheEntryMap() {
        lock.readLock().lock();
        return boardCacheEntryMap;
    }

    public Map<Long, BoardCacheEntry> getFixedBoardCacheMap() {
        lock.readLock().lock();
        return fixedBoardCacheMap;
    }
}
