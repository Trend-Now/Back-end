package com.trend_now.backend.comment.domain;

import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.post.domain.Posts;
import jakarta.persistence.*;
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
public class Comments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Posts posts;

    // BOARD_TTL 시간 안에 작성된 댓글은 boardTtlStatus 필드가 BOARD_TTL_BEFORE, 아니면 BOARD_TTL_AFTER 할당
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "board_ttl_status")
    private BoardTtlStatus boardTtlStatus;
}
