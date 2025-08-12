package com.trend_now.backend.integration.alarm.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.alarm.domain.AlarmSettingType;
import com.trend_now.backend.alarm.domain.AlarmSettings;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class AlarmsSettingTest {

    @Test
    @DisplayName("TOP_10 타입의 AlarmSetting이 기본 시간 05:00으로 생성되는지 테스트")
    void createAlarmSettingWithTop10_DefaultTime() {
        // when
        AlarmSettings alarmSettings = new AlarmSettings(AlarmSettingType.TOP_10, null, null);

        // then
        assertThat(alarmSettings.getAlarmSettingTime()).isEqualTo(LocalTime.of(5, 0));
        assertThat(alarmSettings.getAlarmSettingKeyword()).isNull();
    }

    @Test
    @DisplayName("사용자가 직접 설정한 시간으로 AlarmSetting이 생성되는지 테스트")
    void createAlarmSettingWithCustomTime() {
        // given
        LocalTime customTime = LocalTime.of(7, 30);

        // when
        AlarmSettings alarmSettings = new AlarmSettings(AlarmSettingType.TOP_10, customTime, null);

        // then
        assertThat(alarmSettings.getAlarmSettingTime()).isEqualTo(customTime);
    }

    @Test
    @DisplayName("KEYWORD 타입의 AlarmSetting이 기본값 null로 생성되는지 테스트")
    void createAlarmSettingWithKeyword_DefaultKeyword() {
        // when
        AlarmSettings alarmSettings = new AlarmSettings(AlarmSettingType.KEYWORD, null, null);

        // then
        assertThat(alarmSettings.getAlarmSettingKeyword()).isNull();
        assertThat(alarmSettings.getAlarmSettingTime()).isNull();
    }

    @Test
    @DisplayName("사용자가 직접 키워드를 입력했을 때 올바르게 설정되는지 테스트")
    void createAlarmSettingWithCustomKeyword() {
        // given
        String keyword = "news";

        // when
        AlarmSettings alarmSettings = new AlarmSettings(AlarmSettingType.KEYWORD, null, keyword);

        // then
        assertThat(alarmSettings.getAlarmSettingKeyword()).isEqualTo(keyword);
    }
}
