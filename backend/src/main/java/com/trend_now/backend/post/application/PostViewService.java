package com.trend_now.backend.post.application;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewService {

    private static final String POST_VIEW_COUNT_PREFIX = "post_view_count";
    private static final String NOT_EXIST_POSTS = "게시글이 존재하지 않습니다.";

    private final RedisTemplate<String, String> redisTemplate;
    private final PostsRepository postRepository;


    public void incrementPostView(Long postId) {
        log.info("{}의 조회수를 증가시킵니다.", postId);
        String key = generatePostViewKey(postId);
        // incrementPostView는 getPostViewCount 이후에 실행되기 때문에, key의 존재 여부를 확인하지 않아도 된다.
        redisTemplate.opsForValue().increment(key);
    }

    public int getPostViewCount(Long postId) {
        String key = generatePostViewKey(postId);
        try {
            String viewCount = redisTemplate.opsForValue().get(key);
            // 만약 Redis에 해당 Post의 조회수가 없다면, DB에서 조회수를 가져와 Redis에 저장하고 조회수 리턴
            if (viewCount == null) {
                log.info("Redis에 postId: {}의 조회수가 없습니다. DB에서 조회수를 가져오고 Redis에 세팅합니다.", postId);
                viewCount = String.valueOf(postRepository.findViewCountById(postId));
                redisTemplate.opsForValue().set(key, viewCount);
            }
            return Integer.parseInt(viewCount);
        } catch (Exception e) {
            log.error("Redis에서 장애가 발생하여 조회수를 가져오지 못했습니다. DB에서 조회수를 가져옵니다.", e);
            return postRepository.findViewCountById(postId);
        }

    }

    /**
     * 스케줄러를 통해 주기적으로 DB와 동기화하기 위한 메서드
     */
    @Transactional
    public void syncViewCountToDatabase(Long postId) {
        String key = generatePostViewKey(postId);
        Long currentCount = redisTemplate.opsForValue().increment(key, 0);

        if (currentCount != null && currentCount > 0) {
            Posts posts = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS));
            posts.incrementViewCount();
        }
    }

    private String generatePostViewKey(Long postId) {
        return POST_VIEW_COUNT_PREFIX + ":" + postId;
    }
}
