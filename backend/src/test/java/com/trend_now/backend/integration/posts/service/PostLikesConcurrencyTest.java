package com.trend_now.backend.integration.posts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostLikesIncrementDto;
import com.trend_now.backend.post.repository.PostsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yml")
public class PostLikesConcurrencyTest {

    private static final String REDIS_LIKE_MEMBER_KEY_PREFIX = "post_like_member:";
    private static final String REDIS_LIKE_BOARD_KEY_DELIMITER = ":";

    @Autowired
    private PostLikesService postLikesService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisTemplate<String, String> redisMembersTemplate;

    private Boards boards;
    private Posts posts;
    private List<Members> members;

    @BeforeEach
    public void beforeEach() {
        members = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Members member = Members.builder()
                    .name("testUser" + i)
                    .email("testEmail" + i)
                    .snsId("testSnsId" + i)
                    .provider(Provider.TEST)
                    .build();
            memberRepository.save(member);
            members.add(member);
        }

        boards = Boards.builder()
                .name("testBoard")
                .boardCategory(BoardCategory.REALTIME)
                .build();
        boardRepository.save(boards);

        posts = Posts.builder()
                .title("testTitle")
                .writer(members.get(0).getName())
                .content("testContent")
                .boards(boards)
                .members(members.get(0))
                .build();
        postsRepository.save(posts);

        redisMembersTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @AfterEach
    public void afterEach() {
        postsRepository.deleteAll();
        boardRepository.deleteAll();
    }

    @Test
    @DisplayName("좋아요를 동시에 100명이 누르면 100개가 되어야 한다")
    public void 좋아요_동시성() throws Exception {
        //given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            int idx = i;
            PostLikesIncrementDto postLikesIncrementDto = PostLikesIncrementDto.of(
                    members.get(idx).getName(), boards.getName(), boards.getId(), posts.getId());
            executorService.submit(() -> {
                try {
                    postLikesService.increaseLikeLock(postLikesIncrementDto);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        //then
        int likeCount = redisMembersTemplate.opsForSet()
                .size(REDIS_LIKE_MEMBER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId())
                .intValue();
        assertThat(likeCount).isEqualTo(100);
    }

}
