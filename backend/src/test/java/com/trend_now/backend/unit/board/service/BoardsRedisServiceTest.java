package com.trend_now.backend.unit.board.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.RedisPublisher;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.post.application.PostLikesService;
import com.trend_now.backend.post.application.PostViewService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class BoardsRedisServiceTest {

    private static final String BOARD_RANK_KEY = "board_rank";
    private static final String BOARD_RANK_VALID_KEY = "board_rank_valid";
    private static final String BOARD_THRESHOLD_KEY = "board_threshold";
    private static final String BOARD_INITIAL_COUNT = "0";
    public static final String BOARD_KEY_DELIMITER = ":";
    public static final int BOARD_KEY_PARTS_LENGTH = 2;
    public static final int BOARD_ID_INDEX = 1;
    private static final long KEY_LIVE_TIME = 7201L;
    private static final long BOARD_TIME_UP_50 = 300L;
    private static final long BOARD_TIME_UP_100 = 600L;
    private static final int KEY_EXPIRE = 0;
    private static final int BOARD_TIME_UP_50_THRESHOLD = 50;
    private static final int BOARD_TIME_UP_100_THRESHOLD = 100;
    private static final int POSTS_INCREMENT_UNIT = 1;

    @InjectMocks
    private BoardRedisService boardRedisService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Mock
    private RedisPublisher redisPublisher;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardCache boardCache;

    @Mock
    private Cache<Long, BoardCacheEntry> mockCache;

    @Mock
    private PostLikesService postLikesService;

    @Mock
    private PostViewService postViewService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(boardCache.getBoardCacheEntryMap()).thenReturn(mockCache);
    }

    @ParameterizedTest
    @CsvSource({
            "49, 50, 300",  // 49 → 50개 돌파 → 300초 증가
            "99, 100, 600"  // 99 → 100개 돌파 → 600초 증가
    })
    @DisplayName("실시간 게시판일 때, 게시판의 게시글 수가 일정 개수 이상된다면 해당 게시판의 남은 시간이 증가")
    public void 실시간게시판_일정게시글수_남은시간증가(int initialCount, int threshold, long expectedTimeUp)
            throws Exception {
        //given
        Long boardId = 1L;
        String boardName = "testBoard";
        String key = boardName + BOARD_KEY_DELIMITER + boardId;

        // 실시간 게시판이면서 현재 남은 시간이 100초일 경우
        when(redisTemplate.hasKey(key)).thenReturn(true);
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(100L);

        // 현재 게시글의 수가 49개이면서 시간이 증가된 적이 없을 때
        AtomicLong postCount = new AtomicLong(initialCount);
        doAnswer(invocation -> {
            // Mock을 사용하면 updatePostCountAndExpireTime()에서 increment()가 실행되어도 postCount가 증가하지 않음
            // 1. mock은 실제 구현을 호출하지 않음
            // 2. 내부 상태가 없음
            // 3. 호출 사이의 인과관계가 자동으로 생기지 않음

            // doAnswer()을 사용해 Mock에서도 postCount를 증가시킨다
            Long delta = invocation.getArgument(1);
            return postCount.addAndGet(delta);
        }).when(valueOps).increment(anyString(), anyLong());
        when(valueOps.get(anyString())).thenAnswer(invocation -> String.valueOf(postCount.get()));
        when(setOps.isMember(anyString(), anyString())).thenReturn(false);
        double currentScoreMillis = Instant.now().toEpochMilli();
        when(zSetOps.score(anyString(), eq(key))).thenReturn(currentScoreMillis);

        //when
        boardRedisService.updatePostCountAndExpireTime(boardId, boardName);

        //then
        verify(valueOps).increment(key, 1);
        verify(redisTemplate).expire(key, 100L + expectedTimeUp, TimeUnit.SECONDS);

        Instant expectedScore = Instant.ofEpochMilli((long) currentScoreMillis)
                .plus(expectedTimeUp, ChronoUnit.SECONDS);
        verify(zSetOps).add(eq(BOARD_RANK_KEY), eq(key), eq((double) expectedScore.toEpochMilli()));
        verify(setOps).add(eq(BOARD_THRESHOLD_KEY), contains(String.valueOf(threshold)));
    }

//    @Test
//    @DisplayName("실시간 게시판 목록이 score 기준으로 오름차순 정렬된다")
//    public void 실시간게시판_score_오름차순정렬() throws Exception {
//        //given
//        BoardPagingRequestDto boardPagingRequestDto = new BoardPagingRequestDto(0, 3);
//
//        Set<String> mockBoardKeys = new LinkedHashSet<>();
//        mockBoardKeys.add("board1:1");
//        mockBoardKeys.add("board2:2");
//        mockBoardKeys.add("board3:3");
//
//        when(zSetOps.range(eq(BOARD_RANK_KEY), anyLong(), anyLong()))
//                .thenReturn(mockBoardKeys);
//        List<Long> boardIds = Arrays.asList(1L, 2L, 3L);
//        List<RealtimeBoardDto> mockBoards = Arrays.asList(
//                new RealtimeBoardDto(1L, "board1", 1L, 1L, LocalDateTime.now(),
//                        LocalDateTime.now()),
//                new RealtimeBoardDto(2L, "board2", 1L, 1L, LocalDateTime.now(),
//                        LocalDateTime.now()),
//                new RealtimeBoardDto(3L, "board3", 1L, 1L, LocalDateTime.now(), LocalDateTime.now())
//        );
//
//        when(boardRepository.findRealtimeBoardsByIds(boardIds)).thenReturn(mockBoards);
//
//        List<Object> pipelineResults = Arrays.asList(
//                Arrays.asList(300L, 20.0),   // 게시글1: ttl=300, score=20
//                Arrays.asList(300L, 10.0),   // 게시글2: ttl=300, score=10
//                Arrays.asList(300L, 30.0)    // 게시글3: ttl=300, score=30
//        );
//
//        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(pipelineResults);
//        when(mockCache.estimatedSize()).thenReturn(3L);
//
//        //when
//        BoardPagingResponseDto responseDto = boardRedisService.findAllRealTimeBoardPaging(
//                boardPagingRequestDto);
//
//        //then
//        List<RealtimeBoardDto> sortedBoards = responseDto.getBoardInfoDtos();
//        Assertions.assertThat(sortedBoards).extracting("boardId").containsExactly(2L, 1L, 3L);
//    }

}
