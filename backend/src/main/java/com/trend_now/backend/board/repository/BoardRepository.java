package com.trend_now.backend.board.repository;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.RealtimeBoardListDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BoardRepository extends JpaRepository<Boards, Long> {

    Optional<Boards> findByName(String name);

    List<Boards> findByIdIn(Collection<Long> ids);

    List<Boards> findByBoardCategory(BoardCategory boardCategory);

    @Query("""
        SELECT new com.trend_now.backend.board.dto.RealtimeBoardListDto(
                b.id,
                b.name,
                (SELECT COUNT(p) FROM Posts p WHERE p.boards.id = b.id),
                (SELECT SUM(p.viewCount) FROM Posts p WHERE p.boards.id = b.id),
                b.createdAt,
                b.updatedAt
        )
        FROM Boards b
        WHERE b.id IN :ids
        """)
    List<RealtimeBoardListDto> findRealtimeBoardsByIds(List<Long> ids);
}
