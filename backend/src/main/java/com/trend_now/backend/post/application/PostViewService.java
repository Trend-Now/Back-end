package com.trend_now.backend.post.application;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostViewService {

    private static final String POST_VIEW_COUNT_PREFIX = "post_view_count";
    private static final String NOT_EXIST_POSTS = "게시글이 존재하지 않습니다.";

    private final RedisTemplate<String, String> redisTemplate;
    private final PostsRepository postRepository;


    public void incrementPostView(Long postId) {
        String key = generatePostViewKey(postId);
        redisTemplate.opsForValue().increment(key);
    }

    public int getPostViewCount(Long postId) {
        String key = generatePostViewKey(postId);
        String viewCount = redisTemplate.opsForValue().get(key);
        // 만약 Redis에 해당 Post의 조회수가 없다면, DB에서 조회수를 가져와 Redis에 저장하고 조회수 리턴
        if (viewCount == null) {
            viewCount = String.valueOf(postRepository.findViewCountById(postId));
            redisTemplate.opsForValue().set(key, viewCount);
        }
        // Redis에 조회수가 있다면 해당 값을 그대로 반환
        return Integer.parseInt(viewCount);
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
