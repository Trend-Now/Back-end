package com.trend_now.backend.post.repository;

import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostsRepository extends JpaRepository<Posts, Long> {
    Page<Posts> findAllByBoards_Id(Long boardsId, Pageable pageable);

    void deleteAllByMembers_Id(Long membersId);

    Page<Posts> findByMembers_Id(Long membersId, Pageable pageable);
}
