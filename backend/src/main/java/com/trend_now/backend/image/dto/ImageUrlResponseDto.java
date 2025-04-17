package com.trend_now.backend.image.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUrlResponseDto {
    private String message;
    private List<ImageInfoDto> imageUploadDto;

    public static ImageUrlResponseDto of(String message, List<ImageInfoDto> ImageInfoDtoList) {
        return new ImageUrlResponseDto(message, ImageInfoDtoList);
    }
}
