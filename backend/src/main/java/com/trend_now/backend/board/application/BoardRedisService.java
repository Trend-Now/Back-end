package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.RealTimeBoardTimeUpEvent;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
    private static final String BOARD_THRESHOLD_KEY = "board_threshold";
    private static final String BOARD_INITIAL_COUNT = "0";
    public static final String BOARD_KEY_DELIMITER = ":";
    public static final int BOARD_KEY_PARTS_LENGTH = 2;
    public static final int BOARD_NAME_INDEX = 0;
    public static final int BOARD_ID_INDEX = 1;
    private static final long KEY_LIVE_TIME = 301L;
    private static final long BOARD_TIME_UP_50 = 300L;
    private static final long BOARD_TIME_UP_100 = 600L;
    private static final int KEY_EXPIRE = 0;
    private static final int BOARD_TIME_UP_50_THRESHOLD = 1;
    private static final int BOARD_TIME_UP_100_THRESHOLD = 100;
    private static final int POSTS_INCREMENT_UNIT = 1;

    private static final String NOT_EXIST_BOARD = "선택하신 게시판이 존재하지 않습니다.";

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisPublisher redisPublisher;
    private final BoardRepository boardRepository;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getBoardName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
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
            log.info("{} 게시판의 증가하기 전 남은 시간은 {}입니다", boardName, currentExpireTime);
            String postCountStr = redisTemplate.opsForValue().get(key);
            if (postCountStr == null) {
                return;
            }

            int postCount = Integer.parseInt(postCountStr);
            String threshold50 = boardName + BOARD_KEY_DELIMITER + boardId + BOARD_KEY_DELIMITER
                    + BOARD_TIME_UP_50_THRESHOLD;
            String threshold100 = boardName + BOARD_KEY_DELIMITER + boardId + BOARD_KEY_DELIMITER
                    + (postCount / BOARD_TIME_UP_100_THRESHOLD) * BOARD_TIME_UP_100_THRESHOLD;

            if (postCount == BOARD_TIME_UP_50_THRESHOLD && !Boolean.TRUE.equals(
                    redisTemplate.opsForSet()
                            .isMember(BOARD_THRESHOLD_KEY, threshold50))) {
                log.info("{} 게시판의 게시글 수가 50개 도달, 시간 5분 추가!", boardName);
                extendBoardExpireTime(key, currentExpireTime, BOARD_TIME_UP_50, threshold50);
            } else if (postCount % BOARD_TIME_UP_100_THRESHOLD == 0 && !Boolean.TRUE.equals(
                    redisTemplate.opsForSet()
                            .isMember(BOARD_THRESHOLD_KEY, threshold100))) {
                log.info("{} 게시판의 게시글 수가 100개 도달, 시간 10분 추가!", boardName);
                extendBoardExpireTime(key, currentExpireTime, BOARD_TIME_UP_100, threshold100);
            }
            log.info("{} 게시판의 증가 후 남은 시간은 {}입니다", boardName,
                    redisTemplate.getExpire(key, TimeUnit.SECONDS));
        }
    }

    private void extendBoardExpireTime(String key, Long currentExpireTime, long additionalSeconds,
            String thresholdKey) {
        redisTemplate.expire(key, currentExpireTime + additionalSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(BOARD_THRESHOLD_KEY, thresholdKey);

        String boardName = key.split(Pattern.quote(BOARD_KEY_DELIMITER))[0];
        redisPublisher.publishRealTimeBoardTimeUpEvent(
                RealTimeBoardTimeUpEvent.from(boardName, additionalSeconds));
    }

    /**
     * 게시판에서 게시글이 삭제될 때, Redis에서 게시판의 게시글 수를 업데이트하는 함수
     */
    public void decrementPostCountAndExpireTime(Long boardId, String boardName) {
        String key = boardName + BOARD_KEY_DELIMITER + boardId;
        if (redisTemplate.hasKey(key)) {
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
        Set<String> allRankKey = getBoardRank();
        if (allRankKey == null || allRankKey.isEmpty()) {
            return;
        }

        allRankKey.stream()
                .filter(key -> !Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                .forEach(key -> redisTemplate.opsForZSet().remove(BOARD_RANK_KEY, key));

        Set<String> thresholdSet = redisTemplate.opsForSet().members(BOARD_THRESHOLD_KEY);
        if (thresholdSet == null || thresholdSet.isEmpty()) {
            return;
        }

        thresholdSet.stream()
                .filter(key -> {
                    String boardKey = key.substring(0, key.lastIndexOf(BOARD_KEY_DELIMITER));
                    return !Boolean.TRUE.equals(redisTemplate.hasKey(boardKey));
                })
                .forEach(key -> redisTemplate.opsForSet().remove(BOARD_THRESHOLD_KEY, key));
    }

    /**
     * BoardKeyProvider 인터페이스를 통해서 isRealTimeBoard 메서드에 접근한다. - 특정 DTO에만 종속되는 한계에서 확장성을 고려하여 설계 -
     * isRealTimeBoard 메서드에 접근할려는 DTO는 BoardKeyProvider 인터페이스를 구현체로 진행
     */
    public boolean isRealTimeBoard(BoardKeyProvider provider) {
        String key = provider.getBoardName() + BOARD_KEY_DELIMITER + provider.getBoardId();
        return redisTemplate.hasKey(key);
    }

    // 타이머가 남아있는 게시판이면 true 반환, 고정 게시판이면 false 반환 (true 반환하면 예외 던짐)
    public boolean isNotRealTimeBoard(String boardName, Long boardId, BoardCategory boardCategory) {
        if (boardCategory == BoardCategory.FIXED) {
            return false;
        }
        String key = boardName + BOARD_KEY_DELIMITER + boardId;
        return !redisTemplate.hasKey(key);
    }

    public BoardPagingResponseDto findAllRealTimeBoardPaging(
            BoardPagingRequestDto boardPagingRequestDto) {
        Set<String> allBoardName = getBoardRank();

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
                boardPagingRequestDto.getSize(), Sort.by(Direction.DESC, "createdAt"));
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), boardInfoDtos.size());

        return BoardPagingResponseDto.from(boardInfoDtos.subList(start, end));
    }

    public Set<String> getBoardRank() {
        return redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
    }

    public String getBoardRankValidTime() {
        return redisTemplate.opsForValue().get(BOARD_RANK_VALID_KEY);
    }

    public BoardInfoDto getBoardInfo(Long boardId) {
        Boards findBoard = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        String key = findBoard.getName() + BOARD_KEY_DELIMITER + findBoard.getId();
        Long boardLiveTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return BoardInfoDto.builder()
                .boardId(findBoard.getId())
                .boardName(findBoard.getName())
                .boardLiveTime(boardLiveTime)
                .build();
    }
}
