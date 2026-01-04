package com.trend_now.backend.thread.application;

import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.image.application.ImagesService;
import com.trend_now.backend.image.domain.Images;
import com.trend_now.backend.image.dto.ImageInfoDto;
import com.trend_now.backend.image.repository.ImagesRepository;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.thread.domain.Threads;
import com.trend_now.backend.thread.dto.ThreadsInfoResponseDto;
import com.trend_now.backend.thread.dto.ThreadsSaveDto;
import com.trend_now.backend.thread.repository.ThreadsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springdoc.core.converters.models.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadsService {

    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";

    private final PostsRepository postsRepository;
    private final ThreadsRepository threadsRepository;
    private final ImagesService imagesService;
    private final ImagesRepository imagesRepository;


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

    public List<ThreadsInfoResponseDto> findAllPostThreadsByPostId(Long postId, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size);
        Page<Threads> postThreadsPage = threadsRepository.findPostThreadsPageByPostId(postId, pageable);
        List<Threads> postThreads = postThreadsPage.getContent();
        List<Long> threadIds = postThreads.stream()
            .map(Threads::getId)
            .toList();
        Map<Long, List<ImageInfoDto>> imagesByThreadId = imagesRepository.findAllByThreads_IdIn(threadIds)
            .stream()
            .filter(img -> img.getThreads() != null)
            .collect(Collectors.groupingBy(
                img -> img.getThreads().getId(),
                Collectors.mapping(
                    img -> ImageInfoDto.of(img.getId(), img.getImageUrl()),
                    Collectors.toList()
                )
            ));
        return postThreads.stream()
            .map(t -> ThreadsInfoResponseDto.of(
                t.getId(),
                t.getMembers().getName(),
                imagesByThreadId.getOrDefault(t.getId(), List.of()),
                false,
                false,
                t.getCreatedAt(),
                t.getUpdatedAt()
            ))
            .toList(); 
        }
}
