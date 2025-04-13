package com.trend_now.backend.image.presentation;

import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.dto.ImageUrlResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImagesController {

    private final ImagesService imagesService;

    private final String POSTS_IMAGE_PREFIX = "posts/";

    /**
     * S3에 이미지 업로드 후 DB에 저장하고 이미지 URL 반환
     */
    @PostMapping("/upload")
    public ResponseEntity<ImageUrlResponseDto> uploadImage(
        @RequestPart("images") List<MultipartFile> images) {
        List<String> imageUrls = imagesService.uploadImage(images, POSTS_IMAGE_PREFIX);
        ImageUrlResponseDto response = ImageUrlResponseDto.of(imageUrls);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
