package com.trend_now.backend.thread.dto;

import com.trend_now.backend.common.annotation.ValidContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadsSaveDto {

    @Schema(description = "게시글 쓰레드 식별자로 NULL 가능")
    private final Long parentThreadId;

    @NotEmpty(message = "내용을 입력해주세요.")
    @ValidContent
    private final String threadContent;

    @Size(max = 10, message = "이미지는 10장을 초과할 수 없습니다.")
    private final List<Long> imageIds;

    public static ThreadsSaveDto of(Long parentThreadId, String threadContent, List<Long> imageIds) {
        return new ThreadsSaveDto(parentThreadId, threadContent, imageIds);
    }
}
