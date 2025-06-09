package com.trend_now.backend.main.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trend_now.backend.annotation.WithMockCustomUser;
import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.Top10;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class MainPageControllerTest {

    private static final int BOARD_COUNT = 20;

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

    @BeforeEach
    public void beforeEach() {
        //실시간 검색어 순위 게시판 Setting
        boards = new ArrayList<>();
        top10s = new ArrayList<>();
        for (int i = 0; i < BOARD_COUNT; i++) {
            Boards boards = Boards.builder()
                    .name("B" + i)
                    .boardCategory(BoardCategory.REALTIME)
                    .build();
            Top10 top10 = new Top10(i, "B" + i);
            this.boards.add(boards);
            this.top10s.add(top10);
        }

        redisTemplate.getConnectionFactory().getConnection().flushDb();
        for (int i = 0; i < BOARD_COUNT; i++) {
            BoardSaveDto boardSaveDto = BoardSaveDto.from(top10s.get(i));
            Long boardId = boardService.saveBoardIfNotExists(boardSaveDto);
            boardSaveDto.setBoardId(boardId);
            boardRedisService.saveBoardRedis(boardSaveDto, i);
        }

        //현재 시간 Setting
        boardRedisService.setRankValidListTime();
    }

    @WithMockCustomUser
    @Test
    @DisplayName("로그인된 사용자는 메인 페이지에 접속하였을 때 필요한 정보를 받을 수 있다")
    public void 로그인_사용자_메인페이지_접속() throws Exception {
        //given

        //when

        //then
        mockMvc.perform(get("/api/v1/loadMain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberName").value("testUser"))
                .andDo(print());
    }

    @WithAnonymousUser
    @Test
    @DisplayName("로그인하지 않은 사용자는 메인 페이지에 접속하였을 때 회원 이름을 제외한 정보를 받을 수 있다")
    public void 비로그인_사용자_메인페이지_접속() throws Exception {
        //given

        //when

        //then
        mockMvc.perform(get("/api/v1/loadMain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberName").value("Guest"))
                .andDo(print());
    }

}
