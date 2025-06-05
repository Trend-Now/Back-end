package com.trend_now.backend.image.application;

import com.trend_now.backend.aws.s3.application.S3Service;
import com.trend_now.backend.image.repository.ImagesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class OrphanImageCleaner {

    private final S3Service s3Service;
    private final ImagesRepository imagesRepository;

    /**
     * 24시간 주기로 새벽 3시마다 고아 이미지 삭제
     */
    @Scheduled(cron = "0 0 3 * * *" )
    @Transactional
    protected void deleteOrphanImage() {
        List<String> orphanS3KeyList = imagesRepository.findS3KeyByPostsIsNull();
        log.info("{}개의 고아 이미지 삭제 중", orphanS3KeyList.size());
        s3Service.deleteByS3KeyList(orphanS3KeyList);
        imagesRepository.deleteByPostsIsNull();
        log.info("고아 이미지 삭제 완료");
    }
}
