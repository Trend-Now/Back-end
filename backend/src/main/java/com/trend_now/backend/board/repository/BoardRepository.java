package com.trend_now.backend.board.repository;

import com.trend_now.backend.board.domain.Boards;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Boards, Long> {

    Optional<Boards> findByName(String name);
}
