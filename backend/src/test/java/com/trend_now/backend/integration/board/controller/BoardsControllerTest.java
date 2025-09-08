package com.trend_now.backend.integration.board.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.repository.BoardRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public class BoardsControllerTest {

    private static final String BOARD_RANK_KEY = "board_rank";
    private static final String BOARD_RANK_VALID_KEY = "board_rank_valid";
    private static final String BOARD_RANK_REALTIME_KEY = "board_rank_realtime";
    private static final int BOARD_COUNT = 20;
    private static final long KEY_LIVE_TIME = 301L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRedisService boardRedisService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private List<Boards> boards;
    private List<Top10> top10s;
    @Autowired
    private BoardRepository boardRepository;

    @BeforeEach
    public void before() {
        boards = new ArrayList<>();
        top10s = new ArrayList<>();
        for (int i = 0; i < BOARD_COUNT; i++) {
            Boards boards = Boards.builder()
                    .name("B" + i)
                    .boardCategory(BoardCategory.REALTIME)
                    .build();
            Top10 top10 = new Top10(i, "B" + i, RankChangeType.SAME);
            this.boards.add(boards);
            this.top10s.add(top10);
        }

        redisTemplate.getConnectionFactory().getConnection().flushDb();
        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = BoardSaveDto.from(top10s.get(i));
            Long boardId = boardService.saveBoardIfNotExists(boardSaveDto, RankChangeType.SAME);
            boardSaveDto.setBoardId(boardId);
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1, 5",  // page=0, size=5 -> 총 5개 항목, 전체 20개, 총 4페이지
            "2, 10", // page=1, size=10 -> 총 10개 항목, 전체 20개, 총 2페이지
            "3, 10",  // page=2, size=10 -> 데이터 없음 (out of range)
            "1, 20"  // page=0, size=20 -> 총 20개 항목, 전체 20개, 총 1페이지
    })
    public void getBoards_Pagination_Success(int page, int size) throws Exception {
        mockMvc.perform(get("/api/v1/boards/list")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                )
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("실시간 게시판을 들어갔을 때 실시간 게시판의 정보를 받을 수 있다")
    public void 실시간_게시판_정보() throws Exception {
        //given
        List<Boards> boardList = boardRepository.findAll();
        Long boardId = boardList.getFirst().getId();

        //when

        //then
        mockMvc.perform(get("/api/v1/boards/realtime")
                        .param("boardId", String.valueOf(boardId))
                )
                .andExpect(status().isOk())
                .andDo(print());
    }

}
