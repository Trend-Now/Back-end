package com.trend_now.backend.thread.application;

import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.thread.domain.Threads;
import com.trend_now.backend.thread.dto.ThreadsSaveDto;
import com.trend_now.backend.thread.repository.ThreadsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadsService {

    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";

    private final PostsRepository postsRepository;
    private final ThreadsRepository threadsRepository;
    private final ImagesService imagesService;


    @Transactional
    public void saveThreads(@Valid ThreadsSaveDto threadsSaveDto, Long postId, Members members) {
        Posts posts = postsRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));

        // 쓰레드 저장
        Threads newThread = Threads.builder()
                .posts(posts)
                .parentThreadId(threadsSaveDto.getParentThreadId())
                .members(members)
                .content(threadsSaveDto.getThreadContent())
                .build();

        threadsRepository.save(newThread);

        // 이미지 리스트 저장
        if (threadsSaveDto.getImageIds() != null) {
            threadsSaveDto.getImageIds().forEach(
                    imageId -> {
                        Images newImages = imagesService.findImageById(imageId);
                        newImages.setPosts(posts);
                        newImages.setThreads(newThread);
                    }
            );
        }

    }
}
