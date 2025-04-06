package com.trend_now.backend.member.domain;


import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.domain.Scraps;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_email", columnNames = {"email"}),
    @UniqueConstraint(name = "uk_sns_id", columnNames = {"snsId"}),
    @UniqueConstraint(name = "uk_provider_sns_id", columnNames = {"provider", "snsId"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@ToString
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

    @ToString.Exclude
    @OneToMany(mappedBy = "members", cascade = CascadeType.ALL)
    private List<Posts> posts;

    @ToString.Exclude
    @OneToMany(mappedBy = "members", cascade = CascadeType.ALL)
    private List<Scraps> scraps;
}
