package com.trend_now.backend.post.repository;

import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostsRepository extends JpaRepository<Posts, Long> {

    @Query("""
        SELECT new com.trend_now.backend.post.dto.PostSummaryDto(
                p.id,
                p.title,
                p.writer,
                (SELECT COUNT(c)
                FROM Comments c
                WHERE c.posts.id = p.id),
                p.modifiable,
                p.createdAt,
                p.updatedAt
        )
        FROM Posts p
        WHERE p.boards.id = :boardsId
    """)
    Page<PostSummaryDto> findAllByBoardsId(@Param("boardsId") Long boardsId, Pageable pageable);

    void deleteAllByMembers_Id(Long membersId);

    @Query("""
        SELECT new com.trend_now.backend.post.dto.PostWithBoardSummaryDto(
                p.id,
                p.title,
                p.writer,
                (SELECT COUNT(c) FROM Comments c WHERE c.posts.id = p.id),
                p.modifiable,
                p.createdAt,
                p.updatedAt,
                p.boards.id,
                p.boards.name
        )
        FROM Posts p
        WHERE p.members.id = :membersId
        """)
    Page<PostWithBoardSummaryDto> findByMemberId(@Param("membersId") Long membersId, Pageable pageable);

    // 게시글 제목, 내용에 키워드가 포함된 게시글 중, 실시간 검색어 게시판에 속한 게시물 조회
    @Query("""
        SELECT new com.trend_now.backend.post.dto.PostWithBoardSummaryDto(
                p.id,
                p.title,
                p.writer,
                (SELECT COUNT(c)
                FROM Comments c
                WHERE c.posts.id = p.id),
                p.modifiable,
                p.createdAt,
                p.updatedAt,
                p.boards.id,
                p.boards.name
        )
        FROM Posts p
        WHERE (p.boards.id IN :boardIds)
        AND (p.content LIKE %:keyword% OR p.title LIKE %:keyword%)
        """)
    Page<PostWithBoardSummaryDto> findByKeywordAndRealTimeBoard(
        @Param("keyword") String keyword, @Param("boardIds") Set<Long> boardIds, Pageable pageable);

    @Query("""
        SELECT new com.trend_now.backend.post.dto.PostSummaryDto(
                p.id,
                p.title,
                p.writer,
                (SELECT COUNT(c)
                FROM Comments c
                WHERE c.posts.id = p.id),
                p.modifiable,
                p.createdAt,
                p.updatedAt
        )
        FROM Posts p
        WHERE p.boards.id = :fixBoardId
        AND (p.content LIKE %:keyword% OR p.title LIKE %:keyword%)
        """)
    Page<PostSummaryDto> findByFixBoardsAndKeyword(@Param("keyword") String keyword,
        @Param("fixBoardId") Long fixBoardId, Pageable pageable);

    @Modifying
    @Query("UPDATE Posts p SET p.modifiable = false WHERE p.boards.id = :boardsId AND p.modifiable = true")
    void updateFlagByBoardId(@Param("boardsId") Long boardsId);

    @Query("""
        SELECT new com.trend_now.backend.post.dto.PostsInfoDto(
                p.title,
                p.writer,
                p.content,
                (SELECT COUNT(c)
                FROM Comments c
                WHERE c.posts.id = p.id),
                p.modifiable,
                p.createdAt,
                p.updatedAt
        )
        FROM Posts p
        WHERE p.id = :postId
    """)
    Optional<PostsInfoDto> findPostInfoById(Long postId);

    @Query("SELECT p.viewCount FROM Posts p WHERE p.id = :postId")
    int findViewCountById(Long postId);

    List<Posts> findByIdIn(Collection<Long> ids);

    @Query("SELECT p FROM Posts p JOIN FETCH p.boards WHERE p.id = :postId")
    Optional<Posts> findByIdWithBoard(@Param("postId") Long postId);
}
