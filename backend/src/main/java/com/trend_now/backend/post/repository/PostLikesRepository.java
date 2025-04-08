package com.trend_now.backend.post.repository;

import com.trend_now.backend.post.domain.PostLikes;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikesRepository extends JpaRepository<PostLikes, Long> {

    boolean existsByPostsIdAndMembersId(Long postId, Long memberId);

    Optional<PostLikes> findByPostsIdAndMembersId(Long postId, Long memberId);

    @Query("SELECT pl.members.name FROM PostLikes pl WHERE pl.posts.id = :postId")
    Set<String> findMembersNameByPostsId(@Param("postId") Long postId);
}
