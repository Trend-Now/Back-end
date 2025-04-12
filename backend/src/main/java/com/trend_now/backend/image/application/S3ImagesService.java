package com.trend_now.backend.image.application;

import com.trend_now.backend.aws.s3.application.S3Service;
import com.trend_now.backend.image.domain.S3Images;
import com.trend_now.backend.image.repository.S3ImagesRepository;
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
public class S3ImagesService {

    private final S3ImagesRepository s3ImagesRepository;
    private final S3Service s3Service;
    private final String POSTS_IMAGE_PATH = "posts";


    /**
     * S3에 게시글 이미지 업로드 prefix는 마지막에 '/' 문자를 포함하지 않도록 설정한다.
     */
    @Transactional
    public void uploadPostImage(List<MultipartFile> images, Posts post) throws IOException {
        String prefix = POSTS_IMAGE_PATH + "/" + post.getId();
        // images list가 비어있지 않다면 S3와 DB에 저장
        for (MultipartFile image : images) {
            String s3Key = s3Service.uploadFile(image, prefix, image.getOriginalFilename());
            S3Images saveImage = new S3Images(s3Key, generateImageUrl(s3Key), post);
            s3ImagesRepository.save(saveImage);
        }
    }

    /**
     * S3에서 게시글 이미지 삭제
     */
    @Transactional
    public void deletePostImage(Posts post) {
        List<S3Images> images = s3ImagesRepository.findAllByPosts_Id(post.getId());
        List<String> s3Keys = images.stream()
            .map(S3Images::getS3Key)
            .toList();
        s3Service.deleteFiles(s3Keys);
        s3ImagesRepository.deleteAll(images);
    }

    /**
     * 게시글 이미지 조회
     */
    public List<S3Images> findPostImages(Posts post) {
        return s3ImagesRepository.findAllByPosts_Id(post.getId());
    }

    private String generateImageUrl(String s3Key) {
        return "https://" + s3Service.getBucketName() + ".s3.amazonaws.com/" + s3Key;
    }
}
