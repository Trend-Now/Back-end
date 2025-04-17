package com.trend_now.backend.image.presentation;

import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.image.dto.ImageUploadRequestDto;
import com.trend_now.backend.image.dto.ImageUrlResponseDto;
import io.swagger.v3.oas.annotations.Operation;
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
    private final String IMAGE_UPLOAD_SUCCESS_MESSAGE = "이미지 업로드 성공";

    /**
     * S3에 이미지 업로드 후 DB에 저장하고 이미지 URL 반환
     */
    @PostMapping("/upload")
    @Operation(summary = "이미지 업로드", description = "S3 저장소에 이미지를 업로드 합니다.")
    public ResponseEntity<ImageUrlResponseDto> uploadImage(
        @RequestPart("images") List<MultipartFile> images) {
        List<ImageInfoDto> imageInfoDtoList = imagesService.uploadImage(
            ImageUploadRequestDto.of(images, POSTS_IMAGE_PREFIX));
        ImageUrlResponseDto response = ImageUrlResponseDto.of(IMAGE_UPLOAD_SUCCESS_MESSAGE, imageInfoDtoList);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
