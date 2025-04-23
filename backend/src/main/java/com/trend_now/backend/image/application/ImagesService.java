package com.trend_now.backend.image.application;

import com.trend_now.backend.aws.s3.application.S3Service;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.image.dto.ImageUploadRequestDto;
import com.trend_now.backend.image.repository.ImagesRepository;
import com.trend_now.backend.post.domain.Posts;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImagesService {

    private final ImagesRepository imagesRepository;
    private final S3Service s3Service;

    /**
     * S3와 DB에 저장 후 이미지 URL List 반환
     */
    @Transactional
    public List<ImageInfoDto> uploadImage(ImageUploadRequestDto imageUploadRequestDto) {
        String prefix = imageUploadRequestDto.getPrefix();
        return imageUploadRequestDto.getImages().stream()
            .map(image -> {
                // S3에 업로드
                String s3Key = s3Service.uploadFile(image, prefix, image.getOriginalFilename());
                String imageUrl = generateImageUrl(s3Key);
                // DB에 저장
                Long id = imagesRepository.save(new Images(s3Key, imageUrl)).getId();
                return ImageInfoDto.of(id, imageUrl);
            })
            .toList();
    }

    /**
     * imageId List를 통해 S3와 DB에서 이미지 삭제
     */
    @Transactional
    public void deleteImageByIdList(List<Long> imageId) {
        // S3에서 이미지 삭제
        List<String> s3Keys = imagesRepository.findS3KeyByIdIn(imageId);
        deleteImageByS3KeyList(s3Keys);

        // DB에서 이미지 삭제
        imagesRepository.deleteAllByIdIn(imageId);
    }

    /**
     * S3에서 게시글 이미지 삭제
     */
    @Transactional
    public void deleteImageByPostId(Long postId) {
        // S3에서 이미지 삭제
        List<String> s3Keys = imagesRepository.findS3KeyByPostsId(postId);
        deleteImageByS3KeyList(s3Keys);

        // DB에서 이미지 삭제
        imagesRepository.deleteAllByPosts_Id(postId);
    }

    private void deleteImageByS3KeyList(List<String> s3Keys) {
        if (s3Keys.size() == 1) {
            s3Service.deleteFile(s3Keys.getFirst());
            return;
        }
        s3Service.deleteFiles(s3Keys);
    }

    public Images findImageById(Long id) {
        return imagesRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("해당 이미지를 찾을 수 없습니다."));
    }

    /**
     * 게시글 이미지 조회
     */
    public List<ImageInfoDto> findImagesByPost(Posts post) {
        List<Images> images = imagesRepository.findAllByPosts_Id(post.getId());
        return images.stream().map(
            image -> ImageInfoDto.of(image.getId(), image.getImageUrl())
        ).toList();
    }

    /**
     * 버킷이름과 s3Key를 조합하여 이미지 URL을 생성
     */
    private String generateImageUrl(String s3Key) {
        return "https://" + s3Service.getBucketName() + ".s3.amazonaws.com/" + s3Key;
    }
}
