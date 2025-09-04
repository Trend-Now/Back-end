package com.trend_now.backend.board.repository;

import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardSummaryRepository extends JpaRepository<BoardSummary, Long> {

    Optional<BoardSummary> findByBoards(Boards boards);
}
