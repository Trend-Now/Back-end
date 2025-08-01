package com.trend_now.backend.comment.repository;

import com.trend_now.backend.comment.data.dto.CommentInfoDto;
import com.trend_now.backend.comment.data.dto.FindAllCommentsDto;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.member.domain.Members;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentsRepository extends JpaRepository<Comments, Long> {

    /**
     * <pre>
     * JPQL을 이용한 프로젝션 처리 방식\
     *
     * - DTO 생성자의 파라미터 순서와 SELECT 순서를 지켜줘야 한다.
     * - DTO 객체로 쿼리 결과를 반환하기 위해선 SELECT 부분에 new 키워드와 해당 DTO 경로를 지정해줘야 한다.
     * </pre>
     */
    @Query("""
        SELECT new com.trend_now.backend.comment.data.dto.FindAllCommentsDto(
               c.createdAt, c.updatedAt, c.id, c.content, c.modifiable, c.members.name, c.members.id
            )
        FROM Comments c
        WHERE c.posts.id = :postId
        ORDER BY c.createdAt ASC
        """)
    Page<FindAllCommentsDto> findByPostsIdOrderByCreatedAtAsc(
            @Param("postId") Long postId, Pageable pageable);


    void deleteByIdAndMembers(Long commentId, Members members);

    Optional<Comments> findByIdAndMembers(Long commentId, Members members);

    @Query(
        value = """
            SELECT new com.trend_now.backend.comment.data.dto.CommentInfoDto(
                  p.boards.id, p.id, p.title, c.id, c.content, m.name, c.createdAt
                )
            FROM Comments c
            JOIN c.posts p
            JOIN c.members m
            WHERE c.members.id = :memberId
            """,
        countQuery = """
            SELECT COUNT(c)
            FROM Comments c
            WHERE c.members.id = :memberId
            """
    )
    Page<CommentInfoDto> findByMemberIdWithPost(@Param("memberId") Long membersId,
        Pageable pageable);

    void deleteByPosts_Id(Long postsId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE comments c " +
        "JOIN posts p ON c.post_id = p.post_id " +
        "SET c.modifiable = false " +
        "WHERE p.board_id = :boardId AND c.modifiable = true", nativeQuery = true)
    void updateFlagByBoardId(@Param("boardId") Long boardId);

    // 특정 게시글의 전체 댓글 수 조회
    int countByPostsId(Long postId);
}
