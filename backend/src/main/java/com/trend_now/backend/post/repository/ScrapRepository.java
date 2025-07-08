package com.trend_now.backend.post.repository;

import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.domain.Scraps;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapRepository extends JpaRepository<Scraps, Long> {

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
            FROM Scraps s
            JOIN s.posts p
            WHERE s.members.id = :memberId
        """)
    Page<PostWithBoardSummaryDto> findScrapPostsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    void deleteAllByMembers_Id(Long membersId);

    Optional<Scraps> findByMembersAndPosts(Members members, Posts posts);

    boolean existsScrapsByPosts_IdAndMembers_Id(Long postsId, Long membersId);
}
