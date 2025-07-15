package com.trend_now.backend.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.trend_now.backend.board.application.SignalKeywordService;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithChange;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class SignalKeywordServiceTest {

    private static final String SIGNAL_KEYWORD_LIST = "realtime_keywords";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private SignalKeywordService signalKeywordService;

    @BeforeEach
    public void beforeEach() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    @DisplayName("시스템이 시작되었을 때는 이전의 검색어가 존재하지 않으므로 RankChange를 NEW로 처리한다")
    public void 시스템시작_RankChange_NEW() throws Exception {
        //given
        when(listOperations.range(SIGNAL_KEYWORD_LIST, 0, -1)).thenReturn(null);

        List<Top10> currentTop10 = Arrays.asList(
                new Top10(1, "아이폰"),
                new Top10(2, "갤럭시"),
                new Top10(3, "맥북")
        );

        SignalKeywordDto dto = new SignalKeywordDto();
        dto.setNow(System.currentTimeMillis());
        dto.setTop10(currentTop10);

        //when
        Top10WithChange result = signalKeywordService.calculateRankChange(dto);

        //then
        assertThat(result.getTop10WithDiff()).hasSize(3);
        result.getTop10WithDiff().forEach(diff -> {
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.NEW);
            assertThat(diff.getPreviousRank()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("이전의 검색어에서 순위 변동이 있을 경우 계산하여 반환한다")
    public void 순위_변동() throws Exception {
        //given
        when(listOperations.range(SIGNAL_KEYWORD_LIST, 0, -1)).thenReturn(
                Arrays.asList("갤럭시", "아이폰", "에어팟")
        );

        List<Top10> currentTop10 = Arrays.asList(
                new Top10(1, "아이폰"),   // 기존 2위 → 1위 → UP
                new Top10(2, "에어팟"),   // 기존 3위 → 2위 → UP
                new Top10(3, "갤럭시")    // 기존 1위 → 3위 → DOWN
        );

        SignalKeywordDto dto = new SignalKeywordDto();
        dto.setNow(System.currentTimeMillis());
        dto.setTop10(currentTop10);

        //when
        Top10WithChange result = signalKeywordService.calculateRankChange(dto);

        //then
        assertThat(result.getTop10WithDiff()).hasSize(3);

        assertThat(result.getTop10WithDiff()).anySatisfy(diff -> {
            assertThat(diff.getKeyword()).isEqualTo("아이폰");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.UP);
            assertThat(diff.getPreviousRank()).isEqualTo(2);
        });

        assertThat(result.getTop10WithDiff()).anySatisfy(diff -> {
            assertThat(diff.getKeyword()).isEqualTo("에어팟");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.UP);
            assertThat(diff.getPreviousRank()).isEqualTo(3);
        });

        assertThat(result.getTop10WithDiff()).anySatisfy(diff -> {
            assertThat(diff.getKeyword()).isEqualTo("갤럭시");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.DOWN);
            assertThat(diff.getPreviousRank()).isEqualTo(1);
        });
    }


}
