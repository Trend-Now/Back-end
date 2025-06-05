package com.trend_now.backend.image.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@AllArgsConstructor
public class ImageUploadRequestDto {
    private List<MultipartFile> images;
    private String prefix;

    public static ImageUploadRequestDto of(List<MultipartFile> images, String prefix) {
        return new ImageUploadRequestDto(images, prefix);
    }
}
