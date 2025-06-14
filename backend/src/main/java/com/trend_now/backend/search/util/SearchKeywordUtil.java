package com.trend_now.backend.search.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class SearchKeywordUtil {

    // 완성형 한글의 시작 코드
    private static final char KOREAN_UNICODE_START = '가';
    // 완성형 한글의 마지막 코드
    private static final char KOREAN_UNICODE_END = '힣';
    private static final int JUNGSUNG_COUNT = 21;
    private static final int JONGSUNG_COUNT = 28;

    /**
     * 한글 초성, 중성, 종성 배열 CHOSUNG, JUNGSUNG, JONGSUNG은 Unicode 공식 명칭
     */
    private static final char[] CHOSUNG = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
        'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char[] JUNGSUNG = {
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ',
        'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ',
        'ㅡ', 'ㅢ', 'ㅣ'
    };
    private static final char[] JONGSUNG = {
        '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
        'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    public String disassembleText(String text) {
        StringBuilder initial = new StringBuilder();

        // 한 글자씩 반복을 통해 자음과 모음을 분리한 후 initial 배열에 추가한다.
        for (char c : text.toCharArray()) {
            // 한글이면 자모 분리
            if (isHangul(c)) {
                /**
                 * 한글의 유니코드 구조: 한글 = '가' + (초성 * 21 * 28) + (중성 * 28) + 종성
                 */
                int unicodeValue = c - '가';
                int chosungIndex = unicodeValue / (JUNGSUNG_COUNT * JONGSUNG_COUNT);
                int jungsungIndex = (unicodeValue % (JUNGSUNG_COUNT * JONGSUNG_COUNT)) / JONGSUNG_COUNT;
                int jongsungIndex = unicodeValue % JONGSUNG_COUNT;
                initial.append(CHOSUNG[chosungIndex]);
                initial.append(JUNGSUNG[jungsungIndex]);
                if (jongsungIndex > 0) {
                    initial.append(JONGSUNG[jongsungIndex]);
                }
            }
            // 한글이 아니면 그냥 추가
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
