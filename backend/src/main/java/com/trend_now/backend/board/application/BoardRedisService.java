package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.RealTimeBoardTimeUpEvent;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.application.PostViewService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.redis.core.RedisCallback;
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
    private static final long KEY_LIVE_TIME = 7201L;
    private static final long BOARD_TIME_UP_50 = 300L;
    private static final long BOARD_TIME_UP_100 = 600L;
    private static final int KEY_EXPIRE = 0;
    private static final int BOARD_TIME_UP_50_THRESHOLD = 50;
    private static final int BOARD_TIME_UP_100_THRESHOLD = 100;
    private static final int POSTS_INCREMENT_UNIT = 1;

    private static final String NOT_EXIST_BOARD = "선택하신 게시판이 존재하지 않습니다.";

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisPublisher redisPublisher;
    private final BoardRepository boardRepository;
    private final PostViewService postViewService;
    private final PostLikesService postLikesService;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getBoardName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
        long keyLiveTime = KEY_LIVE_TIME;

        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
            keyLiveTime = currentExpire;
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
        Set<String> allRankKey = getBoardRank(0, -1);
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

    @Transactional // 조회수, 좋아요 개수 데이터 동기화를 하나의 트랜잭션으로 묶는다.
    public BoardPagingResponseDto findAllRealTimeBoardPaging(
        BoardPagingRequestDto boardPagingRequestDto) {
        Set<String> boardKeys = getBoardRank(boardPagingRequestDto.getPage(), boardPagingRequestDto.getSize());

        if (boardKeys == null || boardKeys.isEmpty()) {
            return BoardPagingResponseDto.from(Collections.emptyList());
        }

        // Redis에서 조회한 실시간 게시판 목록 데이터에서 boardId 값만 추출하여 List를 생성한다.
        List<Long> boardIdList = extractBoardIdList(boardKeys);

        // 조회수와 좋아요 개수 동기화
        postViewService.syncViewCountToDatabase();
        postLikesService.syncLikesToDatabase();

        // DB에서 실시간 게시판 목록 조회
        List<RealtimeBoardDto> realtimeBoardList = boardRepository.findRealtimeBoardsByIds(boardIdList);

        // Redis Pipeline을 이용해서 각 Board 별 TTL과 zScore 조회
        List<Object> results = getTtlAndScorePipeline(boardKeys);

        // realtimeBoardList에 ttl과 zScore를 매핑
        IntStream.range(0, realtimeBoardList.size())
            .forEach(i -> {
                RealtimeBoardDto realtimeBoardDto = realtimeBoardList.get(i);
                // result에 ttl과 zScore값이 번갈아가면서 들어있다. {ttl_0, zScore_0, ttl_1, zScore_1 ...}
                realtimeBoardDto.setBoardLiveTime((Long) results.get(i * 2));
                realtimeBoardDto.setScore((Double) results.get(i * 2 + 1));
            });

        // boardLiveTime을 기준으로 1차 정렬, boardLiveTime이 같은 경우 score를 기준으로 2차 정렬
        List<RealtimeBoardDto> sortedRealTimeBoardList = realtimeBoardList.stream()
            .sorted(Comparator.comparingLong(RealtimeBoardDto::getBoardLiveTime).reversed()
                .thenComparingDouble(RealtimeBoardDto::getScore))
            .collect(Collectors.toList());

        return BoardPagingResponseDto.from(sortedRealTimeBoardList);
    }

    private List<Object> getTtlAndScorePipeline(Set<String> boardKeys) {
        List<Object> results = redisTemplate.executePipelined(
            (RedisCallback<Object>) connection -> {
                for (String boardKey : boardKeys) {
                    connection.keyCommands().ttl(boardKey.getBytes());
                    connection.zSetCommands()
                        .zScore(BOARD_RANK_KEY.getBytes(), boardKey.getBytes());
                }
                return null;
            });
        return results;
    }

    private static List<Long> extractBoardIdList(Set<String> boardKeyList) {
        return boardKeyList.stream()
            .map(key -> {
                String[] parts = key.split(BOARD_KEY_DELIMITER);
                if (parts.length < BOARD_KEY_PARTS_LENGTH) {
                    return null;
                }
                return Long.parseLong(parts[BOARD_ID_INDEX]);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public Set<String> getBoardRank(int start, int end) {
        return redisTemplate.opsForZSet().range(BOARD_RANK_KEY, start, end);
    }

    public String getBoardRankValidTime() {
        return redisTemplate.opsForValue().get(BOARD_RANK_VALID_KEY);
    }

    public BoardInfoDto getBoardInfo(Long boardId) {
        Boards findBoard = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_BOARD));

        String key = findBoard.getName() + BOARD_KEY_DELIMITER + findBoard.getId();
        Long boardLiveTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // 게시판 만료 시각을 ms 단위로 계산
        Long expiredBoardTime = System.currentTimeMillis() + (boardLiveTime * 1000);

        return BoardInfoDto.builder()
                .boardId(findBoard.getId())
                .boardName(findBoard.getName())
                .boardLiveTime(boardLiveTime)
                .boardExpiredTime(expiredBoardTime)
                .build();
    }
}
