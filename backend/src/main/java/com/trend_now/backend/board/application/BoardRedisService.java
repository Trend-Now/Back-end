package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardRedisService {

    private static final String BOARD_RANK_KEY = "board_rank";
    private static final String BOARD_RANK_VALID_KEY = "board_rank_valid";
    private static final String BOARD_REALTIME_RANK_KEY = "board_realtime_rank";
    private static final long KEY_LIVE_TIME = 301L;
    private static final int KEY_EXPIRE = 0;

    private final RedisTemplate<String, String> redisTemplate;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getName();
        long keyLiveTime = KEY_LIVE_TIME;

        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
            keyLiveTime += currentExpire;
        }

        redisTemplate.opsForValue().set(key, "실시간 게시글");
        redisTemplate.expire(key, keyLiveTime, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(BOARD_RANK_KEY, key, score);
        redisTemplate.opsForZSet().add(BOARD_REALTIME_RANK_KEY, key, score);
    }

    public void setRankValidListTime() {
        String validTime = Long.toString(
                LocalTime.now().plusSeconds(KEY_LIVE_TIME).toSecondOfDay());
        redisTemplate.opsForValue().set(BOARD_RANK_VALID_KEY, validTime);
    }

    public void cleanUpExpiredKeys() {
        redisTemplate.opsForZSet().removeRange(BOARD_REALTIME_RANK_KEY, 0, -1);

        Set<String> allRankKey = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
        if (allRankKey == null || allRankKey.isEmpty()) {
            return;
        }

        allRankKey.stream()
                .filter(key -> !Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                .forEach(key -> redisTemplate.opsForZSet().remove(BOARD_RANK_KEY, key));
    }

    public boolean isRealTimeBoard(BoardSaveDto boardSaveDto) {
        String boardName = boardSaveDto.getName();
        return redisTemplate.opsForValue().get(boardName) != null;
    }

//    public List<BoardPageRequest> getBoardsWithPaging(PageRequest pageRequest) {
//        Set<String> allBoardName = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
//
//        if (allBoardName == null || allBoardName.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<BoardPageRequest> boardPageRequests = new ArrayList<>();
//
//        for (String boardName : allBoardName) {
//            Long ttl = redisTemplate.getExpire(boardName, TimeUnit.SECONDS);
//            if (ttl != null) {
//                Double score = redisTemplate.opsForZSet().score(BOARD_RANK_KEY, boardName);
//                if (score != null) {
//                    boardPageRequests.add(new BoardPageRequest(boardName, ttl, score));
//                }
//            }
//        }
//
//        boardPageRequests.sort(BoardPageRequest.ttlThenScoreComparator);
//
//        int start = (int) pageRequest.getOffset();
//        int end = (start + pageRequest.getPageSize()) > boardPageRequests.size()
//                ? boardPageRequests.size() : (start + pageRequest.getPageSize());
//        return boardPageRequests.subList(start, end);
//    }

    public BoardPagingResponseDto findAllRealTimeBoardPaging(
            BoardPagingRequestDto boardPagingRequestDto) {
        Set<String> allBoardName = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);

        if (allBoardName == null || allBoardName.isEmpty()) {
            return BoardPagingResponseDto.from(Collections.emptyList());
        }

        List<BoardInfoDto> boardInfoDtos = allBoardName.stream()
                .map(boardName -> {
                    Long boardLiveTime = redisTemplate.getExpire(boardName, TimeUnit.SECONDS);
                    Double score = redisTemplate.opsForZSet().score(BOARD_RANK_KEY, boardName);
                    return new BoardInfoDto(boardName, boardLiveTime, score);
                })
                .sorted(Comparator.comparingLong(BoardInfoDto::getBoardLiveTime).reversed()
                        .thenComparingDouble(BoardInfoDto::getScore))
                .collect(Collectors.toList());

        PageRequest pageRequest = PageRequest.of(boardPagingRequestDto.getPage(),
                boardPagingRequestDto.getSize());
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), boardInfoDtos.size());

        return BoardPagingResponseDto.from(boardInfoDtos.subList(start, end));
    }
}
