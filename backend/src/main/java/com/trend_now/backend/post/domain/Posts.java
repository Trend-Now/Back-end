package com.trend_now.backend.post.domain;

import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.member.domain.Members;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Posts extends BaseEntity {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CONTENT_LENGTH = 10000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(length = MAX_TITLE_LENGTH, nullable = false)
    private String title;

    @Column(nullable = false)
    private String writer;

    @Lob
    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private int viewCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean modifiable = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Boards boards;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Members members;

    public boolean isNotSameId(Long id) {
        return !this.members.getId().equals(id);
    }

    public void changePosts(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void updateViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}
