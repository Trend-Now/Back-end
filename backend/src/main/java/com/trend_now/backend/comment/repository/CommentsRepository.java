package com.trend_now.backend.comment.repository;

import com.trend_now.backend.comment.data.dto.FindAllCommentsDto;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.member.domain.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    /**
     * JPQL을 이용한 프로젝션 처리 방식
     * - DTO 필드와 SELECT 순서를 지켜줘야 한다.
     * - DTO 객체로 쿼리 결과를 반환하기 위해선 SELECT 부분에 new 키워드와 해당 DTO 경로를 지정해줘야 한다.
     */
    @Query("""
            SELECT new com.trend_now.backend.comment.data.dto.FindAllCommentsDto(
                   c.createdAt, c.updatedAt, c.id, c.content, c.boardTtlStatus
                )
            FROM Comments c
            WHERE c.posts.id = :postId
            ORDER BY c.createdAt DESC
            """)
    List<FindAllCommentsDto> findByPostsIdOrderByCreatedAtDesc(
            @Param("postId") Long postId);

    void deleteByIdAndMembers(Long commentId, Members members);

    Optional<Comments> findByIdAndMembers(Long commentId, Members members);
}
