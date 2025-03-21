package com.trend_now.backend.board.repository;

import com.trend_now.backend.board.domain.Board;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByName(String name);
}
