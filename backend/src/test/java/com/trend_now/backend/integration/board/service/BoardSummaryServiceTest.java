package com.trend_now.backend.integration.board.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.application.BoardSummaryService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public class BoardSummaryServiceTest {

    @Autowired
    private BoardSummaryService boardSummaryService;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private BoardSummaryRepository boardSummaryRepository;

    @Test
    @DisplayName("실제 API를 호출하여 키워드 요약을 성공적으로 받아온다.")
    void 키워드_AI_요약_테스트() {
        // given
        String keyword = "오타니";

        // when
        String[] summary = boardSummaryService.summarizeKeyword(keyword);

        // then
        assertThat(summary).isNotNull();
        assertThat(summary).hasSize(2);
        assertThat(summary[0]).isNotBlank();
        assertThat(summary[1]).isNotBlank();
    }

    @Test
    @DisplayName("실제 환경에서 게시판 요약 정보를 저장하고 업데이트한다.")
    void AI_요약_정보_DB_저장_테스트() {
        // given
        String keyword = "오타니";
        Boards board = Boards.builder()
            .name(keyword)
            .boardCategory(BoardCategory.REALTIME)
            .build();
        Boards saveBoard = boardRepository.save(board);

        // when
        boardSummaryService.saveOrUpdateBoardSummary(saveBoard);
        BoardSummary boardSummary = boardSummaryRepository.findByBoards(board).get();

        // then
        assertThat(boardSummary).isNotNull();
        assertThat(boardSummary.getBoards().getId()).isEqualTo(saveBoard.getId());
        assertThat(boardSummary.getSummary()).isNotBlank();
        assertThat(boardSummary.getDetails()).isNotBlank();
    }


}
