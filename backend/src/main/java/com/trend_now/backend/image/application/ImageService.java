package com.trend_now.backend.image.application;

import com.trend_now.backend.aws.s3.S3Service;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.repository.ImagesRepository;
import com.trend_now.backend.post.domain.Posts;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {
    private final ImagesRepository imagesRepository;
    private final S3Service s3Service;
    private final String POSTS_IMAGE_PATH = "posts";

    @Transactional
    public void uploadPostImage(List<MultipartFile> images, Posts post) throws IOException {
        String prefix = POSTS_IMAGE_PATH + "/" + post.getId() + "/";
        // images list가 비어있지 않다면 S3와 DB에 저장
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String imageUrl = s3Service.uploadFile(image, prefix, image.getOriginalFilename());
                Images saveImage = Images.builder()
                    .posts(post)
                    .imageUrl(imageUrl)
                    .build();
                imagesRepository.save(saveImage);
            }
        }
    }
}
