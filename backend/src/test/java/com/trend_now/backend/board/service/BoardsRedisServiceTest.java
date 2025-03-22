package com.trend_now.backend.board.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardPagingRequestDto;
import com.trend_now.backend.board.dto.BoardPagingResponseDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class BoardsRedisServiceTest {

    private static final String BOARD_RANK_KEY = "board_rank";
    private static final String BOARD_RANK_VALID_KEY = "board_rank_valid";
    private static final String BOARD_RANK_REALTIME_KEY = "board_rank_realtime";
    private static final int BOARD_COUNT = 10;
    private static final long KEY_LIVE_TIME = 301L;

    @Autowired
    private BoardRedisService boardRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private List<Boards> boards;

    @BeforeEach
    public void before() {
        boards = new ArrayList<>();
        for (int i = 0; i < BOARD_COUNT; i++) {
            Boards boards = Boards.builder()
                    .name("B" + i)
                    .boardCategory(BoardCategory.REALTIME)
                    .build();
            this.boards.add(boards);
        }

        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("실시간 1순위가 가장 낮은 score로, 301초의 TTL이 redis에 저장된다")
    public void 실시간_1순위_lowest_score와_301초_TTL() throws Exception {
        //given

        //when
        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(boards.get(i).getName(),
                    boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        //then
        //ZSet은 defalut 값이 score이 오름차순 정렬
        //즉, 맨 처음 저장된 게시판 (실시간 1위)이 가장 작은 score을 가진다
        String testBoardKey = "B0";
        Long expireTime = redisTemplate.getExpire(testBoardKey, TimeUnit.SECONDS);
        Set<String> keys = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);

        assertThat(keys).isNotNull();
        assertThat(keys.size()).isEqualTo(BOARD_COUNT);
        assertThat(keys.iterator().next()).isEqualTo(testBoardKey);

        assertThat(expireTime).isNotNull();
        assertThat(expireTime).isLessThan(KEY_LIVE_TIME);
    }

    @Test
    @DisplayName("동일한 키의 값이 갱신되면 301초가 TTL에 추가된다")
    public void 동일한키_301초_추가() throws Exception {
        //given

        //when
        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(boards.get(i).getName(),
                    boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        int testBoardIdx = 0;
        boardRedisService.saveBoardRedis(
                new BoardSaveDto(boards.get(testBoardIdx).getName(),
                        boards.get(testBoardIdx).getBoardCategory()), 0);

        //then
        String testBoardKey = "B0";
        Long expireTime = redisTemplate.getExpire(testBoardKey, TimeUnit.SECONDS);
        assertThat(expireTime).isBetween(KEY_LIVE_TIME, KEY_LIVE_TIME * 2);
    }

    @Test
    @DisplayName("ZSet에 이미 존재한 키가 있을 때 다른 score의 값이 들어올 경우 score가 갱신된다")
    public void score_갱신() throws Exception {
        //given

        //when
        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(boards.get(i).getName(),
                    boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        int testBoardIdx = 0;
        boardRedisService.saveBoardRedis(
                new BoardSaveDto(boards.get(testBoardIdx).getName(),
                        boards.get(testBoardIdx).getBoardCategory()), 5);

        //then
        Set<String> keys = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);

        assertThat(keys).isNotNull();
        assertThat(keys.size()).isEqualTo(BOARD_COUNT);
        assertThat(keys.iterator().next()).isEqualTo("B1");
    }

    @Test
    @DisplayName("실시간 리스트의 마지막 값이 저장될 때 서버에서 갱신 시간을 저장한다")
    public void Redis_게시판_저장() throws Exception {
        //given

        //when
        boardRedisService.setRankValidListTime();

        //then
        String validTime = redisTemplate.opsForValue().get(BOARD_RANK_VALID_KEY);
        assertThat(validTime).isNotNull();

        String max = Long.toString(LocalTime.now().plusSeconds(KEY_LIVE_TIME).toSecondOfDay());
        String min = Long.toString(LocalTime.now().plusSeconds(KEY_LIVE_TIME - 1).toSecondOfDay());

        assertThat(validTime).isBetween(min, max);
    }

    @Test
    @DisplayName("시간이 만료된 RankKey는 ZSet에서 제거한다")
    public void 만료_RankKey_ZSet_제거() throws Exception {
        //given

        //when
        //페이징을 위해 BOARD_RANK_KEY에 대한 ZSet은 만료된 키를 제외하고 남겨둔다

        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(boards.get(i).getName(),
                    boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        String testBoardKey = "B0";
        redisTemplate.expire(testBoardKey, 0L, TimeUnit.SECONDS);

        boardRedisService.cleanUpExpiredKeys();

        //then
        Set<String> allRankKeys = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);

        assertThat(allRankKeys).isNotNull();
        assertThat(allRankKeys.size()).isEqualTo(BOARD_COUNT - 1);
        assertThat(allRankKeys.iterator().next()).isEqualTo("B1");
    }

    @Test
    @DisplayName("스케줄링이 실행될 때 실시간 검색어 순위는 Redis에서 초기화된다")
    public void 실시간_검색어_순위_초기화_Redis() throws Exception {
        //given

        //when
        //실시간 검색어 순위를 저장할 때 10개의 검색어는 BOARD_RANK_KEY와 BOARD_REALTIME_RANK_KEY에 저장된다

        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(boards.get(i).getName(),
                    boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        boardRedisService.cleanUpExpiredKeys();

        //then
        Set<String> allRankKeys = redisTemplate.opsForZSet().range(BOARD_RANK_REALTIME_KEY, 0, -1);
        assertThat(allRankKeys).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "0, 5, B0",  // 첫 번째 페이지, 5개씩 -> 첫 번째 항목은 B0
            "1, 5, B5",  // 두 번째 페이지, 5개씩 -> 첫 번째 항목은 B5
            "2, 5, B10", // 세 번째 페이지, 5개씩 -> 첫 번째 항목은 B10
            "0, 10, B0", // 첫 번째 페이지, 10개씩 -> 첫 번째 항목은 B0
            "1, 10, B10" // 두 번째 페이지, 10개씩 -> 첫 번째 항목은 B10
    })
    @DisplayName("페이지와 사이즈가 주어지면 적절한 페이지를 반환한다")
    public void 페이지네이션_기본기능(int page, int size, String expectedBoardName) throws Exception {
        //given
        int pagination_board_count = 20;
        List<Boards> pagination_boards = new ArrayList<>();
        for (int i = 0; i < pagination_board_count; i++) {
            Boards board = Boards.builder()
                    .name("B" + i)
                    .boardCategory(BoardCategory.REALTIME)
                    .build();
            pagination_boards.add(board);
        }

        for (int i = 0; i < pagination_board_count; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(pagination_boards.get(i).getName(),
                    pagination_boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        //when
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                new BoardPagingRequestDto(page, size));

        //then
        assertThat(allRealTimeBoardPaging).isNotNull();
        assertThat(allRealTimeBoardPaging.getBoardInfoDtos().size()).isEqualTo(size);
        assertThat(allRealTimeBoardPaging.getBoardInfoDtos().getFirst().getBoardName()).isEqualTo(
                expectedBoardName);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 10, B19",  // 첫 번째 페이지(0번 페이지)에서 가장 TTL이 높은 B19가 나와야 함
            "1, 10, B0",   // 두 번째 페이지(1번 페이지)에서 B9가 나와야 함
            "3, 5, B5"     // 네 번째 페이지(3번 페이지)에서 B5가 나와야 함
    })
    @DisplayName("TTL이 큰 순서대로 페이지네이션이 실행되어 반환된다")
    public void TTL_페이지네이션(int page, int size, String expectedBoardName) throws Exception {
        //given
        int pagination_board_count = 20;
        List<Boards> pagination_boards = new ArrayList<>();
        for (int i = 0; i < pagination_board_count; i++) {
            Boards board = Boards.builder()
                    .name("B" + i)
                    .boardCategory(BoardCategory.REALTIME)
                    .build();
            pagination_boards.add(board);
        }

        for (int i = 0; i < pagination_board_count; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(pagination_boards.get(i).getName(),
                    pagination_boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        // 마지막 게시글 10개의 시간을 임의로 증가
        pagination_boards.stream()
                .skip(10)
                .forEach(board -> {
                    Long currentExpire = redisTemplate.getExpire(board.getName(), TimeUnit.SECONDS);
                    if (currentExpire > 0) {
                        currentExpire += (long) (pagination_boards.indexOf(board)
                                * 10);  // i 값을 board의 인덱스 값으로 계산
                    }
                    redisTemplate.expire(board.getName(), currentExpire, TimeUnit.SECONDS);
                });

        //when
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                new BoardPagingRequestDto(page, size));

        //then
        assertThat(allRealTimeBoardPaging).isNotNull();
        assertThat(allRealTimeBoardPaging.getBoardInfoDtos().size()).isEqualTo(size);
        assertThat(allRealTimeBoardPaging.getBoardInfoDtos().getFirst().getBoardName()).isEqualTo(
                expectedBoardName);
    }


    @ParameterizedTest
    @CsvSource({
            "0, 10, B0",   // 첫 번째 페이지(0번 페이지)에서 score이 가장 작은 B0가 나와야 함
            "1, 10, B5",   // 두 번째 페이지(1번 페이지)에서 B5가 나와야 함
            "3, 5, B17"     // 네 번째 페이지(3번 페이지)에서 B17가 나와야 함
    })
    @DisplayName("TTL이 같다면 score은 오름차순 정렬되어 반환된다")
    public void TTL_같을경우_score_오름차순_페이지네이션(int page, int size, String expectedBoardName)
            throws Exception {
        //given
        int pagination_board_count = 20;
        List<Boards> pagination_boards = new ArrayList<>();
        for (int i = 0; i < pagination_board_count; i++) {
            Boards board = Boards.builder()
                    .name("B" + i)
                    .boardCategory(BoardCategory.REALTIME)
                    .build();
            pagination_boards.add(board);
        }

        for (int i = 0; i < pagination_board_count; i++) {
            BoardSaveDto boardSaveDto = new BoardSaveDto(pagination_boards.get(i).getName(),
                    pagination_boards.get(i).getBoardCategory());
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        // 마지막 게시글의 score을 1 ~ 10까지로 변경
        for (int i = 0; i < pagination_board_count - 10; i++) {
            redisTemplate.opsForZSet()
                    .add(BOARD_RANK_KEY, pagination_boards.get(i + 10).getName(), i + 1);
        }

        //when
        BoardPagingResponseDto allRealTimeBoardPaging = boardRedisService.findAllRealTimeBoardPaging(
                new BoardPagingRequestDto(page, size));

        //then
        assertThat(allRealTimeBoardPaging).isNotNull();
        assertThat(allRealTimeBoardPaging.getBoardInfoDtos().size()).isEqualTo(size);
        assertThat(allRealTimeBoardPaging.getBoardInfoDtos().getFirst().getBoardName()).isEqualTo(
                expectedBoardName);
    }

}
