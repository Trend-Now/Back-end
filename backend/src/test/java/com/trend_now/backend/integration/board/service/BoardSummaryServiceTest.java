package com.trend_now.backend.integration.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.trend_now.backend.board.application.board_summary.BoardSummaryTriggerService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import java.time.Duration;
import java.util.Optional;
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
    private BoardSummaryTriggerService boardSummaryTriggerService;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private BoardSummaryRepository boardSummaryRepository;

    @Test
    @DisplayName("실제 환경에서 게시판 요약 정보를 저장하고 업데이트한다.")
    void AI_요약_정보_DB_저장_테스트() {
        // given
        String keyword = "신스";
        Boards board = Boards.builder()
            .name(keyword)
            .boardCategory(BoardCategory.REALTIME)
            .build();
        Boards saveBoard = boardRepository.save(board);

        // when
        boardSummaryTriggerService.triggerSummaryUpdate(saveBoard.getId(), saveBoard.getName(), RankChangeType.NEW);

        // then
        await()
            // 최대 대기 시간
            .atMost(Duration.ofSeconds(10))
            // 0.1초마다 상태 체크
            .with().pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                Optional<BoardSummary> optionalBoardSummary = boardSummaryRepository.findByBoards_Id(
                    saveBoard.getId());
                if (optionalBoardSummary.isPresent()) {
                    BoardSummary boardSummary = optionalBoardSummary.get();
                    assertThat(boardSummary).isNotNull();
                    assertThat(boardSummary.getSummary()).isNotBlank();
                    assertThat(boardSummary.getSummary()).contains(keyword);
                }
            });
    }


}
