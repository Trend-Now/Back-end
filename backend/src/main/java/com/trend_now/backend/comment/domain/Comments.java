package com.trend_now.backend.comment.domain;

import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@ToString
public class Comments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private boolean modifiable = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Members members;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Posts posts;

    public void update(UpdateCommentsDto updateCommentsDto) {
        this.content = updateCommentsDto.getUpdatedComments();
    }

    // 댓글 작성자와 API 요청자가 동일한지 확인하는 메서드
    public boolean isCommentsWriter(Comments comments, Members member) {
        return comments.getMembers().getId().equals(member.getId());
    }
}
