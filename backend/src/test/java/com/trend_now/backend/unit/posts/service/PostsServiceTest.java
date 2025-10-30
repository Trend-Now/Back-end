package com.trend_now.backend.unit.posts.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.repository.PostsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("PostsService 단위 테스트")
public class PostsServiceTest {

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardRedisService boardRedisService;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private HashOperations<Object, Object, Object> hashOperations;

    @InjectMocks
    private PostsService postsService;

    @BeforeEach
    void setUp() {
        // 실제 Redis가 아닌 직접 정의한 Mock 객체를 사용
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("같은 회원이 같은 게시판에서 게시글 작성 이후 5분 이내에 다시 게시글을 작성할 수 없다")
    void 같은_회원이_같은_게시판에서_게시글_작성_이후_5분_이내에_다시_게시글을_작성할_수_없다() {
        // given
        long memberId = 1L;
        long boardId = 1L;
        Members members = Members.builder()
            .id(memberId)
            .name("testUser")
            .email("test123@test.com")
            .build();

        Boards board = Boards.builder()
            .id(boardId)
            .name("testBoard")
            .boardCategory(BoardCategory.REALTIME)
            .build();

        PostsSaveDto dto = PostsSaveDto.of("testTitle", "testContent", null);
        String boardUserKey = String.format("post-cooldown:board:%s:user:%s", boardId, memberId);
        String lastPostTimeKey = "last_post_time";

        // 게시판이 존재하고 실시간 게시판
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));
        when(boardRedisService.isNotRealTimeBoard(any(), any(), any())).thenReturn(false);

        // Redis에 5분 이내 작성한 기록이 있다는 가정
        long recentPostTime = System.currentTimeMillis() - (60 * 1000);
        when(hashOperations.get(boardUserKey, lastPostTimeKey)).thenReturn(recentPostTime);

        // when & then
        // 5분 이내에 다시 게시글을 작성하려고 시도했기 때문에 예외 발생
        assertThrows(IllegalStateException.class, () -> {
            postsService.savePosts(dto, members, boardId);
        });

        // 쿨다운에 걸렸기 때문에 save 메서드가 호출되지 않아야 함
        verify(postsRepository, times(0)).save(any(Posts.class));
    }
}

