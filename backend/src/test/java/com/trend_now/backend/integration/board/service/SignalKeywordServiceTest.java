package com.trend_now.backend.integration.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.trend_now.backend.board.application.SignalKeywordService;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithDiff;
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
        when(redisTemplate.opsForList().rightPush(any(), any())).thenReturn(null);
    }

    @Test
    @DisplayName("시스템이 시작되었을 때는 이전의 검색어가 존재하지 않으므로 RankChange를 NEW로 처리한다")
    public void 시스템시작_RankChange_NEW() throws Exception {
        //given
        List<Top10> currentTop10 = Arrays.asList(
                new Top10(1, "아이폰", RankChangeType.SAME),
                new Top10(2, "갤럭시", RankChangeType.SAME),
                new Top10(3, "맥북", RankChangeType.SAME)
        );
        List<String> response = Arrays.asList("1:아이폰:2:NEW:0", "1:갤럭시:2:NEW:0", "1:맥북:3:NEW:0");

        when(listOperations.range(SIGNAL_KEYWORD_LIST, 0, -1)).thenReturn(response);

        //when
        List<String> result = redisTemplate.opsForList().range(SIGNAL_KEYWORD_LIST, 0, -1);

        //then
        assertThat(result).hasSize(3);
        result.forEach(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.NEW);
            assertThat(diff.getDiffRank()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("이전의 검색어에서 순위 변동이 있을 경우 계산하여 반환한다")
    public void 순위_변동() throws Exception {
        //given
        List<String> response = Arrays.asList("1:갤럭시:2:NEW:0", "2:아이폰:2:NEW:0", "3:에어팟:3:NEW:0");
        when(listOperations.range(SIGNAL_KEYWORD_LIST, 0, -1)).thenReturn(Arrays.asList("1:아이폰:1:UP:2", "2:에어팟:2:UP:3", "3:갤럭시:3:DOWN:1"));

        //when
        List<String> realtimeValueList = redisTemplate.opsForList().range(SIGNAL_KEYWORD_LIST, 0, -1);

        //then
        assertThat(realtimeValueList).hasSize(3);

        assertThat(realtimeValueList).anySatisfy(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getKeyword()).isEqualTo("아이폰");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.UP);
            assertThat(diff.getDiffRank()).isEqualTo(2);
        });

        assertThat(realtimeValueList).anySatisfy(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getKeyword()).isEqualTo("에어팟");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.UP);
            assertThat(diff.getDiffRank()).isEqualTo(3);
        });

        assertThat(realtimeValueList).anySatisfy(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getKeyword()).isEqualTo("갤럭시");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.DOWN);
            assertThat(diff.getDiffRank()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("신규 키워드와 기존 키워드가 혼재된 경우 순위 변동과 NEW 처리를 정상적으로 수행한다")
    public void 기존_신규_혼재_정상처리() throws Exception {
        //given
        when(listOperations.range(SIGNAL_KEYWORD_LIST, 0, -1)).thenReturn(Arrays.asList("1:에어팟:3:UP:2", "2:갤럭시:2:SAME:0", "3:맥북:3:NEW:0"));

        //when
        List<String> realtimeValueList = redisTemplate.opsForList().range(SIGNAL_KEYWORD_LIST, 0, -1);

        //then
        assertThat(realtimeValueList).hasSize(3);

        assertThat(realtimeValueList).anySatisfy(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getKeyword()).isEqualTo("에어팟");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.UP);
            assertThat(diff.getDiffRank()).isEqualTo(2);
        });

        assertThat(realtimeValueList).anySatisfy(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getKeyword()).isEqualTo("갤럭시");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.SAME);
            assertThat(diff.getDiffRank()).isEqualTo(0);
        });

        assertThat(realtimeValueList).anySatisfy(value -> {
            Top10WithDiff diff = Top10WithDiff.from(value);
            assertThat(diff.getKeyword()).isEqualTo("맥북");
            assertThat(diff.getRankChangeType()).isEqualTo(RankChangeType.NEW);
            assertThat(diff.getDiffRank()).isEqualTo(0);
        });
    }
}
