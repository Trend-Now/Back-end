package com.trend_now.backend.integration.posts.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.domain.PostLikes;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostLikesRepository;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class PostLikesServiceUnitTest {

    private static final String REDIS_LIKE_USER_KEY_PREFIX = "post_like_member:";
    private static final String REDIS_LIKE_BOARD_KEY_DELIMITER = ":";
    private static final String BOARD_KEY_DELIMITER = ":";
    private static final String REDIS_LIKE_TIME_UP_PREFIX = "post_like_time_up:";

    @InjectMocks
    private PostLikesService postLikesService;

    @Mock
    private RedisTemplate<String, String> redisMembersTemplate;

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostLikesRepository postLikesRepository;

    @Mock
    private SetOperations<String, String> setOperations;

    @Spy
    private Members members;

    @Spy
    private Posts posts;

    @Spy
    private Boards boards;

    @BeforeEach
    public void beforeEach() {
        members = spy(Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build());

        boards = spy(Boards.builder()
                .id(1L)
                .name("testBoard")
                .boardCategory(BoardCategory.REALTIME)
                .build());

        posts = spy(Posts.builder()
                .id(1L)
                .title("testTitle")
                .writer(members.getName())
                .content("testContent")
                .boards(boards)
                .members(members)
                .build());

        when(redisMembersTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("사용자가 좋아요를 누르지 않은 게시글일 경우에는 좋아요가 추가된다")
    public void 좋아요_추가() throws Exception {
        //given
        when(postsRepository.findById(any(Long.class))).thenReturn(Optional.of(posts));
        when(memberRepository.findByName(any(String.class))).thenReturn(Optional.of(members));

        String redisKey =
                REDIS_LIKE_USER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        String memberName = members.getName();
        when(setOperations.isMember(eq(redisKey), eq(memberName))).thenReturn(false);

        //when
        postLikesService.doLike(boards.getName(), boards.getId(), posts.getId(), members.getName());

        //then
        verify(setOperations, times(1)).add(eq(redisKey), eq(memberName));
        verify(setOperations, never()).remove(eq(redisKey), eq(memberName));
    }

    @Test
    @DisplayName("사용자가 좋아요를 누른 게시글일 경우에는 좋아요가 삭제된다")
    public void 좋아요_삭제() throws Exception {
        //given
        when(postsRepository.findById(any(Long.class))).thenReturn(Optional.of(posts));
        when(memberRepository.findByName(any(String.class))).thenReturn(Optional.of(members));

        String redisKey =
                REDIS_LIKE_USER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        String memberName = members.getName();
        when(setOperations.isMember(eq(redisKey), eq(memberName))).thenReturn(true);

        //when
        postLikesService.doLike(boards.getName(), boards.getId(), posts.getId(), members.getName());

        //then
        verify(setOperations, times(1)).remove(eq(redisKey), eq(memberName));
        verify(setOperations, never()).add(eq(redisKey), eq(memberName));
    }

    @Test
    @DisplayName("좋아요를 누른 사용자가 DB에 존재하지 않을 경우 동기화가 진행된다")
    public void 좋아요_DB_동기화() throws Exception {
        //given
        Long boardId = boards.getId();
        Long postId = posts.getId();

        String redisKey =
                REDIS_LIKE_USER_KEY_PREFIX + boardId + REDIS_LIKE_BOARD_KEY_DELIMITER + postId;
        Set<String> keys = Set.of(redisKey);
        when(redisMembersTemplate.keys(anyString())).thenReturn(keys);

        String memberName = members.getName();
        Set<String> names = Set.of(memberName);
        when(setOperations.members(any(String.class))).thenReturn(names);

        Set<String> dbNames = Set.of();
        when(postLikesRepository.findMembersNameByPostsId(any(Long.class))).thenReturn(dbNames);

        Long memberId = members.getId();
        when(memberRepository.findByName(eq(memberName))).thenReturn(Optional.of(members));
        when(postLikesRepository.existsByPostsIdAndMembersId(eq(postId), eq(memberId))).thenReturn(
                false);

        when(postsRepository.findById(postId)).thenReturn(Optional.of(posts));

        //when
        postLikesService.syncLikesToDatabase();

        //then
        verify(postLikesRepository, times(1)).saveAll(anyList());
        verify(postLikesRepository, never()).delete(any());
    }

    @Test
    @DisplayName("좋아요를 취소한 사용자가 DB에 존재할 경우 좋아요 정보를 삭제한다")
    public void 좋아요취소_DB_동기화() throws Exception {
        //given
        Long boardId = boards.getId();
        Long postId = posts.getId();

        String redisKey =
                REDIS_LIKE_USER_KEY_PREFIX + boardId + REDIS_LIKE_BOARD_KEY_DELIMITER + postId;
        Set<String> keys = Set.of(redisKey);
        when(redisMembersTemplate.keys(anyString())).thenReturn(keys);

        Set<String> names = Set.of();
        when(setOperations.members(any(String.class))).thenReturn(names);

        String memberName = members.getName();
        Set<String> dbNames = Set.of(memberName);
        when(postLikesRepository.findMembersNameByPostsId(any(Long.class))).thenReturn(dbNames);

        Long memberId = members.getId();
        when(memberRepository.findByName(eq(memberName))).thenReturn(Optional.of(members));

        PostLikes postLikes = PostLikes.builder()
                .posts(posts)
                .members(members)
                .build();
        when(postLikesRepository.findByPostsIdAndMembersId(eq(postId), eq(memberId))).thenReturn(
                Optional.of(postLikes));

        //when
        postLikesService.syncLikesToDatabase();

        //then
        verify(postLikesRepository, times(1)).delete(any(PostLikes.class));
    }

    @Test
    @DisplayName("한 게시글의 좋아요가 100개 이상될 때 게시판의 남은 시간이 5분 추가된다")
    public void 좋아요_게시판_시간_추가() throws Exception {
        //given
        when(postsRepository.findById(any(Long.class))).thenReturn(Optional.of(posts));
        when(memberRepository.findByName(any(String.class))).thenReturn(Optional.of(members));

        String redisKey =
                REDIS_LIKE_USER_KEY_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        String memberName = members.getName();
        String timeUpFlagKey =
                REDIS_LIKE_TIME_UP_PREFIX + boards.getId() + REDIS_LIKE_BOARD_KEY_DELIMITER
                        + posts.getId();
        String boardKey = boards.getName() + BOARD_KEY_DELIMITER + boards.getId();

        when(setOperations.isMember(eq(redisKey), eq(memberName))).thenReturn(false);
        when(setOperations.size(eq(redisKey))).thenReturn((long) 100);
        when(redisMembersTemplate.hasKey(eq(timeUpFlagKey))).thenReturn(false);
        when(redisMembersTemplate.getExpire(eq(boardKey), eq(TimeUnit.SECONDS))).thenReturn(301L);

        //when
        postLikesService.doLike(boards.getName(), boards.getId(), posts.getId(), members.getName());

        //then
        verify(setOperations, times(1)).add(eq(redisKey), eq(memberName));
        verify(setOperations, never()).remove(eq(redisKey), eq(memberName));
        verify(redisMembersTemplate, times(1)).expire(eq(boardKey), eq(301L + 301L),
                eq(TimeUnit.SECONDS));
    }

}
