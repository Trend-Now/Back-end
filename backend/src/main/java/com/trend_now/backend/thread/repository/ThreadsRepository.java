package com.trend_now.backend.thread.repository;

import com.trend_now.backend.thread.domain.Threads;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ThreadsRepository extends JpaRepository<Threads, Long> {

    @Query("""
        select t
        from Threads t
        join fetch t.members m
        where t.posts.id = :postId
            and t.parentThreadId is null
        order by t.createdAt desc
        """)
    Page<Threads> findPostThreadsPageByPostId(@Param("postId") Long postId, Pageable pageable);
}
