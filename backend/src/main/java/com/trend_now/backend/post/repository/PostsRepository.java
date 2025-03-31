package com.trend_now.backend.post.repository;

import com.trend_now.backend.post.domain.Posts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostsRepository extends JpaRepository<Posts, Long> {

    Page<Posts> findAllByBoardsId(Long boardId, Pageable pageable);
}
