package com.trend_now.backend.thread.domain;

import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.config.domain.BaseEntity;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;

import jakarta.annotation.Nullable;
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
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class Threads extends BaseEntity{

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CONTENT_LENGTH = 10000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thread_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Posts posts;

    /**
     * 쓰레드는 게시글 쓰레드(부모 쓰레드), 답글 쓰레드(자식 쓰레드) 존재
     * 게시글 쓰레드의 경우 parent_thread_id는 null
     * 답글 쓰레드의 경우 parent_thread_id는 게시글 쓰레드의 id
     */
    @Column(name = "parent_thread_id", nullable = true)
    @Nullable
    private Long parentThreadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Members members;

    @Lob
    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;
}