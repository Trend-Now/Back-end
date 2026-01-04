package com.trend_now.backend.image.application;

import com.trend_now.backend.infra.s3.application.S3Service;
import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.image.dto.ImageUploadRequestDto;
import com.trend_now.backend.image.repository.ImagesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImagesService {

    private final ImagesRepository imagesRepository;
    private final S3Service s3Service;

    /**
     * S3와 DB에 저장 후 이미지 URL List 반환
     */
    @Transactional
    public List<ImageInfoDto> uploadImage(ImageUploadRequestDto imageUploadRequestDto) {
        String prefix = imageUploadRequestDto.getPrefix();
        log.info("ImagesService - uploadImage: 이미지 업로드 시작. prefix={}", prefix);
        return imageUploadRequestDto.getImages().stream()
            .map(image -> {
                // S3에 업로드
                String s3Key = s3Service.uploadFile(image, prefix, image.getOriginalFilename());
                String imageUrl = generateImageUrl(s3Key);
                // DB에 저장
                Long id = imagesRepository.save(
                    Images.builder()
                        .s3key(s3Key)
                        .imageUrl(imageUrl)
                        .build()
                    ).getId();
                return ImageInfoDto.of(id, imageUrl);
            })
            .toList();
    }

    public Images findImageById(Long id) {
        return imagesRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("해당 이미지를 찾을 수 없습니다."));
    }

    /**
     * 게시글 이미지 조회
     */
    public List<ImageInfoDto> findImagesByPost(Long postId) {
        List<Images> images = imagesRepository.findAllByPosts_Id(postId);
        return images.stream().map(
            image -> ImageInfoDto.of(image.getId(), image.getImageUrl())
        ).toList();
    }

    /**
     * imageId List를 통해 S3와 DB에서 이미지 삭제
     */
    @Transactional
    public void deleteImageById(Long imageId) {
        // S3에서 이미지 삭제
        String s3Key = imagesRepository.findS3KeyById(imageId);
        s3Service.deleteFile(s3Key);

        // DB에서 이미지 삭제
        imagesRepository.deleteById(imageId);
    }

    /**
     * imageId List를 통해 S3와 DB에서 이미지 삭제
     */
    @Transactional
    public void deleteImageByIdList(List<Long> imageId) {
        // S3에서 이미지 삭제
        List<String> s3Keys = imagesRepository.findS3KeyByIdIn(imageId);
        s3Service.deleteByS3KeyList(s3Keys);

        // DB에서 이미지 삭제
        imagesRepository.deleteAllByIdIn(imageId);
    }

    /**
     * S3에서 게시글 이미지 삭제
     */
    @Transactional
    public void deleteImageByPostId(Long postId) {
        List<String> s3Keys = imagesRepository.findS3KeyByPostsId(postId);
        if (s3Keys != null && !s3Keys.isEmpty()) {
            // S3에서 이미지 삭제
            s3Service.deleteByS3KeyList(s3Keys);
            // DB에서 이미지 삭제
            imagesRepository.deleteAllByPosts_Id(postId);
        }
    }

    /**
     * 버킷이름과 s3Key를 조합하여 이미지 URL을 생성
     */
    private String generateImageUrl(String s3Key) {
        return "https://" + s3Service.getBucketName() + ".s3.amazonaws.com/" + s3Key;
    }
}
