package com.trend_now.backend.alarm.domain;


import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalTime;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class AlarmSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_setting_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmSettingType alarmSettingType;

    private LocalTime alarmSettingTime;
    private String alarmSettingKeyword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public AlarmSetting(AlarmSettingType alarmSettingType, LocalTime alarmSettingTime,
            String alarmSettingKeyword) {
        this.alarmSettingType = alarmSettingType;
        this.alarmSettingTime = Optional.ofNullable(alarmSettingTime)
                .orElseGet(() -> alarmSettingType.getAlarmSettingTime().orElse(null));
        this.alarmSettingKeyword = Optional.ofNullable(alarmSettingKeyword)
                .orElseGet(() -> alarmSettingType.getAlarmSettingKeyword().orElse(null));
    }
}
