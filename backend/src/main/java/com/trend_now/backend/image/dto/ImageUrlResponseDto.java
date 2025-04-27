package com.trend_now.backend.image.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageUrlResponseDto {
    private final String message;
    private final List<ImageInfoDto> imageUploadDto;

    public static ImageUrlResponseDto of(String message, List<ImageInfoDto> ImageInfoDtoList) {
        return new ImageUrlResponseDto(message, ImageInfoDtoList);
    }
}
