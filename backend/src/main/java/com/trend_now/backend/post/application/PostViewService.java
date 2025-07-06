package com.trend_now.backend.post.application;

import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String REDIS_POST_VIEW_COUNT_KEY_DELIMITER = ":";
    private static final int POST_ID_INDEX = 1;

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
        String viewCount = redisTemplate.opsForValue().get(key);
        // 만약 Redis에 해당 Post의 조회수가 없다면, DB에서 조회수를 가져와 Redis에 저장하고 조회수 리턴
        if (viewCount == null) {
            log.info("Redis에 postId: {}의 조회수가 없습니다. DB에서 조회수를 가져오고 Redis에 세팅합니다.", postId);
            int viewCountById = postRepository.findViewCountById(postId);
            viewCount = String.valueOf(viewCountById);
            redisTemplate.opsForValue().set(key, viewCount);
        }
        return Integer.parseInt(viewCount);
    }

    /**
     * 스케줄러를 통해 주기적으로 DB와 동기화하기 위한 메서드
     */
    @Transactional
    public void syncViewCountToDatabase() {
        // Redis에서 조회수를 동기화할 게시글 키를 모두 가져온다.
        Set<String> keys = redisTemplate.keys(
            POST_VIEW_COUNT_PREFIX + REDIS_POST_VIEW_COUNT_KEY_DELIMITER
                + "*");
        // multiGet은 keys의 순서에 따라 조회수를 가져오므로, keys와 viewCountList의 순서가 보장된다.
        List<String> viewCountList = redisTemplate.opsForValue().multiGet(keys);
        // 이후에 동기화 작업 중 해당 게시글에 대해 새로운 조회수가 발생할 수 있으므로, 조회 이후에 즉시 삭제한다.
        redisTemplate.delete(keys);

        if (keys.isEmpty()) {
            log.info("조회수를 동기화할 데이터가 없습니다.");
            return;
        }

        // 데이터를 파싱하여 Key: PostId, Value: viewCount 형태 Map으로 변환
        Map<Long, Integer> map = new HashMap<>();
        int currentIndex = 0;
        for (String key : keys) {
            String viewCount = viewCountList.get(currentIndex++);
            if (viewCount == null) {
                log.warn("Redis에 저장된 조회수가 없습니다. key: {}", key);
                continue;
            }
            String[] postIdStr = key.split(REDIS_POST_VIEW_COUNT_KEY_DELIMITER);
            Long postId = Long.parseLong(postIdStr[POST_ID_INDEX]);
            map.put(postId, Integer.parseInt(viewCount));
        }

        // Redis에 저장된 조회수를 DB에 동기화
        List<Posts> postList = postRepository.findByIdIn(map.keySet());
        postList.forEach(post -> post.updateViewCount(map.get(post.getId())));

        log.info("게시글 조회수를 Redis에서 DB로 동기화했습니다. {}개의 게시글이 업데이트되었습니다.", postList.size());
    }

    private String generatePostViewKey(Long postId) {
        return POST_VIEW_COUNT_PREFIX + REDIS_POST_VIEW_COUNT_KEY_DELIMITER + postId;
    }
}
