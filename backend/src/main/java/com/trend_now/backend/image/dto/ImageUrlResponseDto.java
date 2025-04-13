package com.trend_now.backend.image.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUrlResponseDto {
    List<String> imageUrls;

    public static ImageUrlResponseDto of(List<String> imageUrls) {
        return new ImageUrlResponseDto(imageUrls);
    }
}
