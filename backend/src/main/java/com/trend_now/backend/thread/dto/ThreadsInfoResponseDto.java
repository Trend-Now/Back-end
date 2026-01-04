package com.trend_now.backend.thread.dto;

import com.trend_now.backend.image.dto.ImageInfoDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadsInfoResponseDto {

    private final Long threadId;
    private final String threadWriter;
    private final List<ImageInfoDto> imageInfos;
    private final Boolean isThreadWriter;
    private final Boolean isThreadLike;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ThreadsInfoResponseDto of(
        Long threadId,
        String threadWriter,
        List<ImageInfoDto> imageInfos,
        Boolean isThreadWriter,
        Boolean isThreadLike,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    ThreadsInfoResponseDto dto =
        new ThreadsInfoResponseDto(threadId, threadWriter, imageInfos, isThreadWriter, isThreadLike);
    dto.createdAt = createdAt;
    dto.updatedAt = updatedAt;
    return dto;
    }
}
