package com.trend_now.backend.board.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
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

}
