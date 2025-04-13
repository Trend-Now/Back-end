package com.trend_now.backend.post.application;

import com.trend_now.backend.config.RedissonConfig;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.domain.PostLikes;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostLikesRepository;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikesService {

    private static final String NOT_EXIST_POSTS = "게시글이 존재하지 않습니다.";
    private static final String NOT_EXIST_MEMBERS = "회원을 찾을 수 없습니다.";
    private static final String NOT_EXIST_LIKES = "좋아요 객체가 존재하지 않습니다.";
    private static final String REDIS_LIKE_MEMBER_KEY_PREFIX = "post_like_member:";
    private static final String REDIS_LIKE_BOARD_KEY_DELIMITER = ":";
    private static final String REDIS_LIKE_LOCK_PREFIX = "POST_LIKES_LOCK";
    private static final String REDIS_LIKE_TIME_UP_PREFIX = "post_like_time_up:";
    private static final int BOARD_KEY_PARTS_LENGTH = 2;
    private static final int BOARD_ID_IDX = 0;
    private static final int POST_ID_IDX = 1;
    private static final int WAIT_MILLI_SEC = 10000;
    private static final int RELEASE_MILLI_SEC = 10000;
    private static final Integer LIKE_INITIAL_COUNT = 0;
    private static final int LIKE_TIME_UP_LIMIT = 100;
    private static final String BOARD_KEY_DELIMITER = ":";
    private static final long POST_LIKES_TIME_UP = 301L;
    private static final int KEY_EXPIRE = 0;

    private final PostsRepository postsRepository;
    private final MemberRepository memberRepository;
    private final PostLikesRepository postLikesRepository;
    private final RedisTemplate<String, String> redisMembersTemplate;
    private final RedissonConfig redissonConfig;

    @Transactional
    public void saveLike(Long postId, Long memberId) {
        Posts posts = postsRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

        Members members = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_MEMBERS));

        PostLikes postLikes = PostLikes.builder()
                .posts(posts)
                .members(members)
                .build();
        postLikesRepository.save(postLikes);
    }

    @Transactional
    public void deleteLike(Long postId, Long memberId) {
        PostLikes postLikes = postLikesRepository.findByPostsIdAndMembersId(postId, memberId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_LIKES));
        postLikesRepository.delete(postLikes);
    }

    /**
     * 좋아요의 개수로 게시판의 시간이 결정되기 때문에 '좋아요의 개수'는 매우 중요하다 여러 사용자가 동시에 좋아요 버튼을 누르더라도 좋아요가 올바르게 눌려야 한다
     */
    public void increaseLikeLock(String boardName, Long boardId, Long postId, String name) {
        String lockName =
                REDIS_LIKE_LOCK_PREFIX + boardId + REDIS_LIKE_BOARD_KEY_DELIMITER + postId;
        redissonConfig.execute(lockName, WAIT_MILLI_SEC, RELEASE_MILLI_SEC,
                () -> doLike(boardName, boardId, postId, name));
    }

    public void doLike(String boardName, Long boardId, Long postId, String name) {
        log.info("게시판 {}에 있는 게시글 {}에 회원 {}이 좋아요를 눌렀습니다.", boardId, postId, name);

        postsRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));

        memberRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_MEMBERS));

        String redisKey =
                REDIS_LIKE_MEMBER_KEY_PREFIX + boardId + REDIS_LIKE_BOARD_KEY_DELIMITER + postId;

        Boolean isRedisPresent = redisMembersTemplate.opsForSet().isMember(redisKey, name);

        /**
         * Write Back 패턴 사용(Redis를 주로 사용하고, DB는 주기적으로 업데이트)
         * isRedisPresent : 사용자가 게시글에 좋아요를 눌렀는지 Redis에서 확인한다
         * true를 반환할 경우 이미 좋아요를 누른 게시글
         */
        if (Boolean.TRUE.equals(isRedisPresent)) {
            //이미 좋아요를 누른 경우
            log.info("{}가 이미 좋아요를 누른 게시글이므로 좋아요가 취소 처리된다", name);
            redisMembersTemplate.opsForSet().remove(redisKey, name);
        } else {
            //좋아요를 누른 게시글이 아닐 경우
            log.info("{}가 좋아요를 누르지 않은 게시글이므로 좋아요 개수가 증가한다", name);
            redisMembersTemplate.opsForSet().add(redisKey, name);

            String timeUpFlagKey =
                    REDIS_LIKE_TIME_UP_PREFIX + boardId + REDIS_LIKE_BOARD_KEY_DELIMITER + postId;
            Boolean hasSetTimeUp = redisMembersTemplate.hasKey(timeUpFlagKey);
            /**
             * 좋아요가 게시글에 눌릴 때 100개 이상이 되면 게시글이 속한 게시판의 시간이 5분 추가된다
             */
            if (Boolean.FALSE.equals(hasSetTimeUp)
                    && redisMembersTemplate.opsForSet().size(redisKey) >= LIKE_TIME_UP_LIMIT) {
                log.info("게시판 {}의 좋아요 개수가 100개 이상이 되었을 때, 게시판의 시간이 5분 추가된다", boardId);
                String boardKey = boardName + BOARD_KEY_DELIMITER + boardId;
                long keyLiveTime = POST_LIKES_TIME_UP;

                Long currentExpire = redisMembersTemplate.getExpire(boardKey, TimeUnit.SECONDS);
                if (currentExpire != null && currentExpire > KEY_EXPIRE) {
                    keyLiveTime += currentExpire;
                }

                redisMembersTemplate.expire(boardKey, keyLiveTime, TimeUnit.SECONDS);
            }
        }
    }

    @Transactional
    public void syncLikesToDatabase() {
        //Write-Back 전략을 사용해 현재 Redis에 저장된 좋아요를 DB에 업데이트한다
        Set<String> keys = redisMembersTemplate.keys(REDIS_LIKE_MEMBER_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            log.info("동기화할 좋아요 데이터가 없습니다.");
            return;
        }

        List<PostLikes> postLikesList = new ArrayList<>();
        for (String key : keys) {
            String[] parts = key.replace(REDIS_LIKE_MEMBER_KEY_PREFIX, "")
                    .split(REDIS_LIKE_BOARD_KEY_DELIMITER);
            if (parts.length != BOARD_KEY_PARTS_LENGTH) {
                log.warn("잘못된 Redis 키 {} 형식의 데이터가 존재합니다.", key);
                continue;
            }

            Long boardId = Long.parseLong(parts[BOARD_ID_IDX]);
            Long postId = Long.parseLong(parts[POST_ID_IDX]);

            Set<String> names = redisMembersTemplate.opsForSet().members(key);
            if (names == null) {
                names = Set.of();
            }

            Set<String> dbNames = postLikesRepository.findMembersNameByPostsId(postId);

            /**
             * 좋아요를 누른 사용자가
             * - DB에 존재하지 않을 꼉우, DB에 좋아요 정보를 저장
             * - DB에 존재할 경우, 로직을 계속 진행
             *
             * 좋아요를 취소한 사용자가
             * - DB에 존재할 경우, DB에서 좋아요 정보를 삭제
             * - DB에 존재하지 않을 경우, 로직을 게속 진행
             */

            for (String name : names) {
                memberRepository.findByName(name).ifPresent(members -> {
                    Posts posts = postsRepository.findById(postId)
                            .orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_POSTS));
                    boolean alreadyLiked = postLikesRepository.existsByPostsIdAndMembersId(postId,
                            members.getId());
                    if (!alreadyLiked) {
                        PostLikes postLikes = PostLikes.builder()
                                .posts(posts)
                                .members(members)
                                .build();
                        postLikesList.add(postLikes);
                        log.info("DB에 게시글 번호 {}에 대한 회원 {}의 좋아요를 저장합니다", postId, name);
                    }
                });
            }

            for (String dbName : dbNames) {
                if (!names.contains(dbName)) {
                    memberRepository.findByName(dbName).ifPresent(members -> {
                        deleteLike(postId, members.getId());
                        log.info("DB에 게시글 번호 {}에 대한 회원 {}의 좋아요를 삭제합니다", postId, dbName);
                    });
                }
            }
        }

        if (!postLikesList.isEmpty()) {
            postLikesRepository.saveAll(postLikesList);
        }
    }

    /**
     * Redis에서 장애가 발생했을 때, DB에서 좋아요 수를 읽어와야 한다 함수형 인터페이스를 사용해 장애 상황을 대처한다
     */
    private <T> T withFallback(Supplier<T> primary, Supplier<T> fallback) {
        try {
            log.info("Look Aside Redis 읽기 성공, 좋아요 수를 Redis에서 읽어옵니다");
            return primary.get();
        } catch (Exception e) {
            log.warn("Look Aside Redis 읽기 실패, DB에서 좋아요 읽기 수행 중 : {}", e.getMessage());
            return fallback.get();
        }
    }

    public int getPostLikesCount(Long boardId, Long postId) {
        String redisKey =
                REDIS_LIKE_MEMBER_KEY_PREFIX + boardId + REDIS_LIKE_BOARD_KEY_DELIMITER + postId;

        return withFallback(
                () -> {
                    Integer likeCount = Optional.ofNullable(
                                    redisMembersTemplate.opsForSet().size(redisKey))
                            .map(Long::intValue)
                            .orElse(LIKE_INITIAL_COUNT);

                    return likeCount.intValue();
                },
                () -> getPostLikesFromDatabase(redisKey, boardId, postId)
        );
    }

    public int getPostLikesFromDatabase(String redisKey, Long boardId, Long postId) {
        if (!postsRepository.existsById(postId)) {
            throw new IllegalArgumentException(NOT_EXIST_POSTS);
        }

        Set<PostLikes> postLikesSet = postLikesRepository.findByPostsId(postId);

        try {
            updateRedisLikeCount(redisKey, postLikesSet);
            log.info("DB에서 가져온 좋아요 개수를 Redis에 성공적으로 업데이트했습니다");
        } catch (Exception e) {
            log.warn("DB에서 가져온 좋아요 개수를 Redis에 업데이트하는 데 실패했습니다 : {}", e.getMessage());
        }

        return postLikesSet.size();
    }

    private void updateRedisLikeCount(String redisKey, Set<PostLikes> likeCount) {
        for (PostLikes postLikes : likeCount) {
            redisMembersTemplate.opsForSet().add(redisKey, postLikes.getMembers().getName());
        }
    }
}
