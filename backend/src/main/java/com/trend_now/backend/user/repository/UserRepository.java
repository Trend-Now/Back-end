package com.trend_now.backend.user.repository;


import com.trend_now.backend.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
}
