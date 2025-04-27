package com.trend_now.backend.image.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageInfoDto {

    private final Long id;
    private final String imageUrl;

    public static ImageInfoDto of(Long id, String imageUrl) {
        return new ImageInfoDto(id, imageUrl);
    }
}
