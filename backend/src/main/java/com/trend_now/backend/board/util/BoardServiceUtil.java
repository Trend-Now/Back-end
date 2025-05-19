package com.trend_now.backend.board.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class BoardServiceUtil {



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
