package com.trend_now.backend.common.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.common.annotation.ValidContent;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.QuillDeltaDto;
import com.trend_now.backend.post.dto.QuillDeltaDto.QuillOp;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContentValidator implements ConstraintValidator<ValidContent, String> {

    // ConstraintValidator는 hibernate validator에서 관리하므로 싱글톤으로 동작함
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isValid(String content, ConstraintValidatorContext context) {
        // 게시글의 내용이 비어있으면 저장할 수 없다
        if (content == null || content.isEmpty()) {
            return false;
        }
        try {
            // String 타입의 content를 QuillDeltaDto 형식으로 변경
            QuillDeltaDto delta = objectMapper.readValue(content, QuillDeltaDto.class);
            int totalTextLength = 0;
            for (QuillOp op : delta.getOps()) {
                // insert 값이 String이라면 content (content가 아닌 값은 Map 형식으로 되어 있음)
                if (op.getInsert() instanceof String) {
                    totalTextLength += ((String) op.getInsert()).length();
                    if (totalTextLength > Posts.MAX_CONTENT_LENGTH) {  // 텍스트 길이 제한 초과
                        return false;
                    }
                }
            }
        } catch (JsonProcessingException e) { // JSON 파싱 에러
            throw new RuntimeException(e);
        }
        return true;
    }
}
