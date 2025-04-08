package com.trend_now.backend.post.repository;

import com.trend_now.backend.post.domain.Scraps;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapRepository extends JpaRepository<Scraps, Long> {

    @EntityGraph(attributePaths = "posts")
    Page<Scraps> findScrapsByMembers_Id(Long membersId, Pageable pageable);

    void deleteAllByMembers_Id(Long membersId);
}
