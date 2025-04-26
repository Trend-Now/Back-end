package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private static final String BOARD_INITIAL_COUNT = "0";
    private static final long KEY_LIVE_TIME = 301L;
    private static final long BOARD_TIME_UP_50 = 300L;
    private static final long BOARD_TIME_UP_100 = 600L;
    private static final int KEY_EXPIRE = 0;
    private static final int BOARD_TIME_UP_50_THRESHOLD = 50;
    private static final int BOARD_TIME_UP_100_THRESHOLD = 100;

    private static final String BOARD_KEY_DELIMITER = ":";
    private static final int BOARD_KEY_PARTS_LENGTH = 2;
    private static final int BOARD_NAME_INDEX = 0;
    private static final int BOARD_ID_INDEX = 1;
    private static final int POSTS_INCREMENT_UNIT = 1;

    private final RedisTemplate<String, String> redisTemplate;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
        long keyLiveTime = KEY_LIVE_TIME;

        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
            keyLiveTime += currentExpire;
        }

        redisTemplate.opsForValue().set(key, BOARD_INITIAL_COUNT);
        redisTemplate.expire(key, keyLiveTime, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(BOARD_RANK_KEY, key, score);
    }

    /**
     * 실시간 게시판일 때, 게시판의 게시글 수가 일정 개수 이상된다면 해당 게시판의 남은 시간이 증가
     */
    public void updatePostCountAndExpireTime(Long boardId, String boardName) {
        String key = boardName + BOARD_KEY_DELIMITER + boardId;
        if (redisTemplate.hasKey(key)) {
            Long currentExpireTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            redisTemplate.opsForValue().increment(key, POSTS_INCREMENT_UNIT);
            redisTemplate.expire(key, currentExpireTime, TimeUnit.SECONDS);
            log.info("{} 게시판의 증가하기 전 남은 시간은 {}입니다", boardName, currentExpireTime);

            String postCountStr = redisTemplate.opsForValue().get(key);
            if (postCountStr == null) {
                return;
            }

            int postCount = Integer.parseInt(postCountStr);
            if (postCount == BOARD_TIME_UP_50_THRESHOLD) {
                log.info("{} 게시판의 게시글 수가 50개 도달, 시간 5분 추가!", boardName);
                redisTemplate.expire(key, currentExpireTime + BOARD_TIME_UP_50, TimeUnit.SECONDS);
                log.info("{} 게시판의 증가 후 남은 시간은 {}입니다", boardName,
                        redisTemplate.getExpire(key, TimeUnit.SECONDS));
            } else if (postCount >= BOARD_TIME_UP_100_THRESHOLD && postCount % BOARD_TIME_UP_100_THRESHOLD == 0) {
                log.info("{} 게시판의 게시글 수가 100개 도달, 시간 10분 추가!", boardName);
                redisTemplate.expire(key, currentExpireTime + BOARD_TIME_UP_100, TimeUnit.SECONDS);
                log.info("{} 게시판의 증가 후 남은 시간은 {}입니다", boardName,
                        redisTemplate.getExpire(key, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * 게시판에서 게시글이 삭제될 때, Redis에서 게시판의 게시글 수를 업데이트하는함수
     */
    public void decrementPostCountAndExpireTime(Long boardId, String boardName) {
        String key = boardName + BOARD_KEY_DELIMITER + boardId;
        if(redisTemplate.hasKey(key)) {
            Long currentExpireTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            redisTemplate.opsForValue().decrement(key, POSTS_INCREMENT_UNIT);
            redisTemplate.expire(key, currentExpireTime, TimeUnit.SECONDS);
        }
    }

    public void setRankValidListTime() {
        String validTime = Long.toString(
                LocalTime.now().plusSeconds(KEY_LIVE_TIME).toSecondOfDay());
        redisTemplate.opsForValue().set(BOARD_RANK_VALID_KEY, validTime);
    }

    public void cleanUpExpiredKeys() {
        Set<String> allRankKey = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
        if (allRankKey == null || allRankKey.isEmpty()) {
            return;
        }

        allRankKey.stream()
                .filter(key -> !Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                .forEach(key -> redisTemplate.opsForZSet().remove(BOARD_RANK_KEY, key));
    }

    public boolean isRealTimeBoard(BoardSaveDto boardSaveDto) {
        String key = boardSaveDto.getName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
        return redisTemplate.hasKey(key);
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
                    log.info("[BoardRedisService.findAllRealTimeBoardPaging] : parts = {}",
                            Arrays.toString(parts));

                    // 데이터 타입 이상 시, 다음으로 넘김
                    if (parts.length < BOARD_KEY_PARTS_LENGTH) {
                        return null;
                    }

                    String boardName = parts[BOARD_NAME_INDEX];
                    Long boardId = Long.parseLong(parts[BOARD_ID_INDEX]);

                    Long boardLiveTime = redisTemplate.getExpire(boardKey, TimeUnit.SECONDS);
                    Double score = redisTemplate.opsForZSet().score(BOARD_RANK_KEY, boardKey);
                    return new BoardInfoDto(boardId, boardName, boardLiveTime, score);
                })
                .filter(Objects::nonNull)
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
