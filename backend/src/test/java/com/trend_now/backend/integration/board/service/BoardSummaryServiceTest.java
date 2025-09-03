package com.trend_now.backend.integration.board.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.board.application.BoardSummaryService;
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

    @Test
    @DisplayName("실제 API를 호출하여 키워드 요약을 성공적으로 받아온다.")
    void summarizeKeyword_withRealApi_success() {
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
}
