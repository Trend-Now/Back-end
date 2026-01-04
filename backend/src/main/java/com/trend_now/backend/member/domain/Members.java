package com.trend_now.backend.member.domain;


import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.image.domain.Images;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sns_id", columnNames = {"snsId"}),
    @UniqueConstraint(name = "uk_provider_sns_id", columnNames = {"provider", "snsId"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@ToString(exclude = "profileImage")
public class Members extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Setter
    @Column(nullable = false, name = "name")
    private String name;

    @Column(nullable = false, name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "provider")
    private Provider provider;

    @Column(nullable = false, name = "sns_id")
    private String snsId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private Role role = Role.USER;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "image_id")
    private Images profileImage;

    public void updateProfileImage(Images profileImage) {
        this.profileImage = profileImage;
    }
}
