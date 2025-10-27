package com.trend_now.backend.unit.common.validator;

import com.trend_now.backend.common.validator.ContentValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentValidatorTest {
    private final ContentValidator validator = new ContentValidator();

    @ParameterizedTest
    @DisplayName("게시글 내용 글자수 제한 테스트")
    @CsvSource({
        "100, true",           // 정상: 짧은 텍스트
        "9999, true",          // 정상: 경계값 미만
        "10000, true",         // 정상: 경계값 정확히
        "10001, false",        // 실패: 경계값 초과
        "15000, false"         // 실패: 많이 초과
    })
    void testContentLengthValidation(int textLength, boolean expectedValid) {
        // given - 실제 Quill 에디터에서 생성되는 형태의 JSON
        String mainText = "a".repeat(textLength);
        String quillDeltaJson = String.format(
            "{\"ops\":[" +
                "{\"insert\":\"%s\", \"attributes\":{\"bold\":true}}," +  // content
                "{\"insert\":{\"image\":\"https://example.com/image.png\"}}" +  // 이미지는 글자수 카운트 X
            "]}",
            mainText
        );

        // when
        boolean isValid = validator.isValid(quillDeltaJson, null);

        // then
        if (expectedValid) {
            assertTrue(isValid);
        } else {
            assertFalse(isValid);
        }
    }
}