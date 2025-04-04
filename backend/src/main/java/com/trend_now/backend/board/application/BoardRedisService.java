package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BoardRedisService {

    private static final String BOARD_RANK_KEY = "board_rank";
    private static final String BOARD_RANK_VALID_KEY = "board_rank_valid";
    private static final String BOARD_REALTIME_RANK_KEY = "board_realtime_rank";
    private static final long KEY_LIVE_TIME = 301L;
    private static final int KEY_EXPIRE = 0;

    private static final String BOARD_KEY_DELIMITER  = ":";
    private static final int BOARD_KEY_PARTS_LENGTH = 2;
    private static final int BOARD_NAME_INDEX = 0;
    private static final int BOARD_ID_INDEX = 1;

    private final RedisTemplate<String, String> redisTemplate;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
        long keyLiveTime = KEY_LIVE_TIME;

        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
            keyLiveTime += currentExpire;
        }

        redisTemplate.opsForValue().set(key, "실시간 게시판");
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

    public BoardPagingResponseDto findAllRealTimeBoardPaging(
            BoardPagingRequestDto boardPagingRequestDto) {
        Set<String> allBoardName = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);

        if (allBoardName == null || allBoardName.isEmpty()) {
            return BoardPagingResponseDto.from(Collections.emptyList());
        }

        List<BoardInfoDto> boardInfoDtos = allBoardName.stream()
                .map(boardKey -> {

                    // boardId 추출
                    String[] parts = boardKey.split(BOARD_KEY_DELIMITER);
                    log.info("[BoardRedisService.findAllRealTimeBoardPaging] : parts = {}", Arrays.toString(parts));

                    // 데이터 타입 이상 시, 다음으로 넘김
                    if(parts.length < BOARD_KEY_PARTS_LENGTH) return null;

                    String boardName = parts[BOARD_NAME_INDEX];
                    Long boardId = Long.parseLong(parts[BOARD_ID_INDEX]);

                    Long boardLiveTime = redisTemplate.getExpire(boardKey, TimeUnit.SECONDS);
                    Double score = redisTemplate.opsForZSet().score(BOARD_RANK_KEY, boardKey);
                    return new BoardInfoDto(boardId, boardName, boardLiveTime, score);
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

    public String getBoardRankValidTime() {
        return redisTemplate.opsForValue().get(BOARD_RANK_VALID_KEY);
    }
}
