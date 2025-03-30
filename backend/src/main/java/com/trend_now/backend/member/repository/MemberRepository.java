package com.trend_now.backend.member.repository;


import com.trend_now.backend.member.domain.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Members, Long> {

    Optional<Members> findBySnsId(String socialId);
}
