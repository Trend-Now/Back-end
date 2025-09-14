package com.trend_now.backend.board.application;

import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.RealTimeBoardTimeUpEvent;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.application.PostViewService;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
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
    private final BoardCache boardCache;
    private final BoardSummaryRepository boardSummaryRepository;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, double score) {
        String key = boardSaveDto.getBoardName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
        long keyLiveTime = KEY_LIVE_TIME;

        // 기존 키의 TTL이 남아 있을 경우 새로운 시간이 할당되지 않고, 기존 시간이 그대로 유지되는 버그 때문에 주석 처리
//        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
//        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
//            keyLiveTime = currentExpire;
//        }

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
        // 실시간 게시판의 남은 시간(TTL)이 증가
        redisTemplate.expire(key, currentExpireTime + additionalSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(BOARD_THRESHOLD_KEY, thresholdKey);

        // TTL이 증가하는 만큼 score도 증가
        Double currentScore = redisTemplate.opsForZSet()
            .score(BOARD_RANK_KEY, key); // 현재 저장된 만료 시간을 가져옴
        if (currentScore == null) {
            return; // 실시간 게시판이 아직 등록되지 않은 경우
        }

        // 실시간 게시판의 score로 증가된 시간이 세팅
        Instant currentExpireScore = Instant.ofEpochMilli(currentScore.longValue());
        Instant newExpireScore = currentExpireScore.plus(additionalSeconds, ChronoUnit.SECONDS);
        redisTemplate.opsForZSet().add(BOARD_RANK_KEY, key, newExpireScore.toEpochMilli());

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
        int page = boardPagingRequestDto.getPage(); // 0
        int size = boardPagingRequestDto.getSize(); // 5
        // Redis에서의 조회는 0부터 시작하므로, size로 받은 숫자에서 1을 뺀 값을 사용한다.
        int start = page * size; // 0
        int end = start + size - 1; // 4
        List<TypedTuple<String>> boardRankList = getBoardRankList(start, end);

        if (boardRankList == null || boardRankList.isEmpty()) {
            return BoardPagingResponseDto.from(0, 0, Collections.emptyList());
        }
        // Redis에서 조회한 실시간 게시판 목록 데이터에서 boardId 값만 추출하여 List를 생성한다.
        List<Long> boardIdList = extractBoardIdList(boardRankList);

        // DB에서 실시간 게시판 목록 조회 후 Map으로 변환
        Map<Long, RealtimeBoardDto> realtimeBoardMap = boardRepository.findRealtimeBoardsByIds(
            boardIdList).stream().collect(
            Collectors.toMap(RealtimeBoardDto::getBoardId, realtimeBoard -> realtimeBoard));

        // 조회수와 좋아요 개수 동기화
        postViewService.syncViewCountToDatabase();
        postLikesService.syncLikesToDatabase();

        // realtimeBoardList에 ttl과 zScore를 매핑
        long now = Instant.now().toEpochMilli();
        List<RealtimeBoardDto> realtimeBoardList = IntStream.range(0, boardIdList.size())
            .mapToObj(i -> {
                Long boardId = boardIdList.get(i);
                RealtimeBoardDto realtimeBoardDto = realtimeBoardMap.get(boardId);
                Double score = boardRankList.get(i).getScore();
                // boardLiveTime는 score의 정수부를 long으로 변환하여 현재 시간과 차이를 계산 (ms 단위)
                long boardLiveTimeInMs = (score.longValue() * -1) - now;
                // boardLiveTime을 초 단위로 변환하여 설정
                realtimeBoardDto.setBoardLiveTime(boardLiveTimeInMs / 1000);
                // score는 page가 0일 경우 0 ~ 9, page가 1일 경우 10 ~ 19, ... 와 같이 설정
                realtimeBoardDto.setScore((i + 1) + (page * 10));

                return realtimeBoardDto;
            }).toList();

        long realtimeBoardCount = boardCache.getBoardCacheEntryMap()
            .estimatedSize(); // 캐시에서 실시간 게시판 목록의 사이즈 조회
        long totalPages = (long) Math.ceil(
            (double) realtimeBoardCount / boardPagingRequestDto.getSize());
        return BoardPagingResponseDto.from(totalPages, realtimeBoardCount, realtimeBoardList);
    }

    private static List<Long> extractBoardIdList(List<TypedTuple<String>> boardRankSet) {
        return boardRankSet.stream()
            .map(value -> {
                String key = value.getValue();
                String[] parts = key.split(BOARD_KEY_DELIMITER);
                if (parts.length < BOARD_KEY_PARTS_LENGTH) {
                    return null;
                }
                return Long.parseLong(parts[BOARD_ID_INDEX]);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public List<TypedTuple<String>> getBoardRankList(int start, int end) {
        Set<TypedTuple<String>> boardRankSet = redisTemplate.opsForZSet()
            .rangeWithScores(BOARD_RANK_KEY, start, end);
        if (boardRankSet == null || boardRankSet.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(boardRankSet);
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
        Double score = redisTemplate.opsForZSet().score(BOARD_RANK_KEY, key);

        // 게시판의 남은 시간 (ms 단위)
        long now = Instant.now().toEpochMilli();
        long boardLiveTime = ((score.longValue() * -1) - now);

        // AI 요약 정보 조회
        BoardSummary boardSummary = boardSummaryRepository.findByBoards_Id(findBoard.getId())
            .orElse(BoardSummary.builder()
                .summary(null)
                .build());

        return BoardInfoDto.builder()
            .boardId(findBoard.getId())
            .boardName(findBoard.getName())
            .boardLiveTime(boardLiveTime / 1000) // 초 단위로 변환
            .boardExpiredTime(score)
            .summary(boardSummary.getSummary())
            .build();
    }
}
