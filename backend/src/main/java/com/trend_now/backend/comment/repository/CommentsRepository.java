package com.trend_now.backend.comment.repository;

import com.trend_now.backend.comment.data.vo.FindAllComments;
import com.trend_now.backend.comment.domain.Comments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    List<FindAllComments> findByPostsIdOrderByCreatedAtDesc(Long postId);
}
