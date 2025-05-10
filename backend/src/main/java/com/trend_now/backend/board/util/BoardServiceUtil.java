package com.trend_now.backend.board.util;

import com.trend_now.backend.board.dto.BoardInfoDto;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class BoardServiceUtil {

    public static final String BOARD_KEY_DELIMITER = ":";
    public static final String BOARD_RANK_KEY = "board_rank";
    public static final int BOARD_KEY_PARTS_LENGTH = 2;
    public static final int BOARD_NAME_INDEX = 0;
    public static final int BOARD_ID_INDEX = 1;

    private final RedisTemplate<String, String> redisTemplate;

    // String을 입력 받아 BoardInfoDto로 변환하는 함수
    public Function<String, BoardInfoDto> getStringBoardInfoDto() {
        return boardKey -> {
            // boardId 추출
            String[] parts = boardKey.split(BOARD_KEY_DELIMITER);
            log.info("[BoardRedisService.findAllRealTimeBoardPaging] : parts = {}",
                Arrays.toString(parts));

            // 데이터 타입 이상 시, 다음으로 넘김
            if (parts.length < BOARD_KEY_PARTS_LENGTH)
                return null;

            String boardName = parts[BOARD_NAME_INDEX];
            Long boardId = Long.parseLong(parts[BOARD_ID_INDEX]);

            Long boardLiveTime = redisTemplate.getExpire(boardKey, TimeUnit.SECONDS);
            Double score = redisTemplate.opsForZSet().score(BOARD_RANK_KEY, boardKey);
            return new BoardInfoDto(boardId, boardName, boardLiveTime, score);
        };
    }

}
