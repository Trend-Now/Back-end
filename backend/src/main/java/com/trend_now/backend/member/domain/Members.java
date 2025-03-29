package com.trend_now.backend.member.domain;


import com.trend_now.backend.config.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uk_sns_id", columnNames = {"snsId"}),
        @UniqueConstraint(name = "uk_provider_sns_id", columnNames = {"provider", "snsId"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Members extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(nullable = false, name = "email")
    private String email;

    @Column(nullable = false, name = "provider")
    private String provider;

    @Column(nullable = false, name = "sns_id")
    private String snsId;
}
