package com.trend_now.backend.posts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.ArrayList;
import java.util.List;
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
@Transactional
public class PostLikesServiceTest {

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

    @Test
    @DisplayName("Redis Cache Hit일 경우 Redis에서 좋아요 수를 반환한다")
    public void Redis_좋아요_수() throws Exception {
        //given
        String redisKey =
                REDIS_LIKE_MEMBER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        redisMembersTemplate.opsForSet().add(redisKey, "testUser1", "testUser2", "testUser3");

        //when
        int postLikesCount = postLikesService.getPostLikesCount(boards.getId(), posts.getId());

        //then
        assertThat(postLikesCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Redis Cache Miss 발생 시 DB에서 좋아요 수를 조회한다")
    public void DB_좋아요_수() throws Exception {
        //given
        String redisKey =
                REDIS_LIKE_MEMBER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        for (int i = 0; i < 5; i++) {
            postLikesService.increaseLikeLock(boards.getName(), boards.getId(), posts.getId(),
                    members.get(i).getName());
        }
        postLikesService.syncLikesToDatabase();

        //when
        int postLikesFromDatabase = postLikesService.getPostLikesFromDatabase(redisKey,
                boards.getId(),
                posts.getId());

        //then
        assertThat(postLikesFromDatabase).isEqualTo(5);
    }

    @Test
    @DisplayName("Redis Cache Miss 발생 시 Redis 좋아요 수를 DB의 값으로 업데이트한다")
    public void Redis_DB_동기화() throws Exception {
        //given
        for (int i = 0; i < 10; i++) {
            postLikesService.increaseLikeLock(boards.getName(), boards.getId(), posts.getId(),
                    members.get(i).getName());
        }
        postLikesService.syncLikesToDatabase();

        String redisKey =
                REDIS_LIKE_MEMBER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        redisMembersTemplate.delete(redisKey);

        //when
        postLikesService.getPostLikesFromDatabase(redisKey, boards.getId(), posts.getId());

        //then
        int likeCount = redisMembersTemplate.opsForSet().size(redisKey).intValue();
        assertThat(likeCount).isEqualTo(10);
    }
}
