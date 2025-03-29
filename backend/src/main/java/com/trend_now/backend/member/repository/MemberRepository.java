package com.trend_now.backend.member.repository;


import com.trend_now.backend.member.domain.Members;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Members, Long> {
}
