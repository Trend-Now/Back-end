package com.trend_now.backend.image.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageInfoDto {

    private Long id;
    private String imageUrl;

    public static ImageInfoDto of(Long id, String imageUrl) {
        return new ImageInfoDto(id, imageUrl);
    }
}
