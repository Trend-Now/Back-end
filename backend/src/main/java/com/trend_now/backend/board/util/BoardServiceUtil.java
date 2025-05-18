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

    // 완성형 한글의 시작 코드
    private static final char KOREAN_UNICODE_START = '가';
    private static final char KOREAN_UNICODE_END = '힣';

    /**
     * 한글 초성, 중성, 종성 배열
     * CHOSUNG, JUNGSUNG, JONGSUNG은 Unicode 공식 명칭
     */
    private static final char[] CHOSUNG = {
        'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ',
        'ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };
    private static final char[] JUNGSUNG = {
        'ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ',
        'ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ',
        'ㅡ','ㅢ','ㅣ'
    };
    private static final char[] JONGSUNG = {
        '\0','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ',
        'ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ',
        'ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };

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

    public String disassembleText(String text) {
        StringBuilder initial = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (isHangul(c)) {
                int unicodeValue = c - KOREAN_UNICODE_START;
                int chosungIndex = unicodeValue / (21 * 28);
                int jungsungIndex = (unicodeValue % (21 * 28)) / 28;
                int jongsungIndex = unicodeValue % 28;
                initial.append(CHOSUNG[chosungIndex]);
                initial.append(JUNGSUNG[jungsungIndex]);
                if (jongsungIndex > 0) {
                    initial.append(JONGSUNG[jongsungIndex]);
                }
            }
            else {
                initial.append(c);
            }
        }
        return initial.toString();
    }

    private boolean isHangul(char c) {
        return c >= KOREAN_UNICODE_START && c <= KOREAN_UNICODE_END;
    }
}
