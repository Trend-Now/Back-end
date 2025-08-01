package com.trend_now.backend.member.repository;


import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Members, Long> {

    Optional<Members> findBySnsId(String socialId);

    Optional<Members> findByEmail(String email);

    boolean existsByName(String name);

    Optional<Members> findByName(String name);

    List<Members> findAllBySnsId(String testSnsId);

    Optional<Members> findBySnsIdAndProvider(String snsId, Provider provider);
}
