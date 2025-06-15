package com.trend_now.backend.post.repository;

import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.domain.Scraps;
import java.util.List;
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
            SELECT s.posts
            FROM Scraps s
            JOIN s.posts p
            JOIN FETCH p.boards
            WHERE s.members.id = :memberId
        """)
    Page<Posts> findScrapPostsByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    void deleteAllByMembers_Id(Long membersId);

    Optional<Scraps> findByMembersAndPosts(Members members, Posts posts);
}
