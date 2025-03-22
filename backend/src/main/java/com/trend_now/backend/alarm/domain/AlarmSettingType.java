package com.trend_now.backend.alarm.domain;

import java.time.LocalTime;
import java.util.Optional;

public enum AlarmSettingType {
    TOP_10(LocalTime.of(5, 0), null),
    KEYWORD(null, null);

    private final LocalTime alarmSettingTime;
    private final String alarmSettingKeyword;

    AlarmSettingType(LocalTime alarmSettingTime, String alarmSettingKeyword) {
        this.alarmSettingTime = alarmSettingTime;
        this.alarmSettingKeyword = alarmSettingKeyword;
    }

    public Optional<LocalTime> getAlarmSettingTime() {
        return Optional.ofNullable(alarmSettingTime);
    }

    public Optional<String> getAlarmSettingKeyword() {
        return Optional.ofNullable(alarmSettingKeyword);
    }
}
