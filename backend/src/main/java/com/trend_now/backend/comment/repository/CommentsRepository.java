package com.trend_now.backend.comment.repository;

import com.trend_now.backend.comment.data.vo.FindAllComments;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.member.domain.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    List<FindAllComments> findByPostsIdOrderByCreatedAtDesc(Long postId);

    void deleteByIdAndMembers(Long commentId, Members members);

    Optional<Comments> findByIdAndMembers(Long commentId, Members members);
}
