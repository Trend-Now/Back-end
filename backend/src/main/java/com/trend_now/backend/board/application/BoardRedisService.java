package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
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
    public static final String BOARD_KEY_DELIMITER = ":";
    public static final int BOARD_KEY_PARTS_LENGTH = 2;
    public static final int BOARD_NAME_INDEX = 0;
    public static final int BOARD_ID_INDEX = 1;
    private static final long KEY_LIVE_TIME = 301L;
    private static final int KEY_EXPIRE = 0;

    private static final String NOT_EXIST_BOARD = "선택하신 게시판이 존재하지 않습니다.";

    private final RedisTemplate<String, String> redisTemplate;
    private final BoardRepository boardRepository;

    public void saveBoardRedis(BoardSaveDto boardSaveDto, int score) {
        String key = boardSaveDto.getBoardName() + BOARD_KEY_DELIMITER + boardSaveDto.getBoardId();
        long keyLiveTime = KEY_LIVE_TIME;

        Long currentExpire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (currentExpire != null && currentExpire > KEY_EXPIRE) {
            keyLiveTime += currentExpire;
        }

        redisTemplate.opsForValue().set(key, "실시간 게시판");
        redisTemplate.expire(key, keyLiveTime, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(BOARD_RANK_KEY, key, score);
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
