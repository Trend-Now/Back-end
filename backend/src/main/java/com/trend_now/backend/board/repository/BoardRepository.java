package com.trend_now.backend.board.repository;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
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
        SELECT new com.trend_now.backend.board.dto.RealtimeBoardDto(
                b.id,
                b.name,
                COALESCE(COUNT(p.id), 0),
                COALESCE(SUM(p.viewCount), 0),
                b.createdAt,
                b.updatedAt
            )
        FROM Boards b
        LEFT JOIN Posts p ON p.boards.id = b.id
        WHERE b.id IN :ids
        GROUP BY b.id, b.name, b.createdAt, b.updatedAt
        """)
    List<RealtimeBoardDto> findRealtimeBoardsByIds(List<Long> ids);

    @Query("""
        SELECT new com.trend_now.backend.board.dto.RealtimeBoardDto(
                b.id,
                b.name,
                COALESCE(COUNT(p.id), 0),
                COALESCE(SUM(p.viewCount), 0),
                b.createdAt,
                b.updatedAt
            )
            FROM Boards b
            LEFT JOIN Posts p ON p.boards.id = b.id
            WHERE b.id = :boardId
            GROUP BY b.id, b.name, b.createdAt, b.updatedAt
        """)
    RealtimeBoardDto findRealtimeBoardById(Long boardId);


    @Query("SELECT b.name FROM Boards b WHERE b.id = :id")
    String findNameById(Long id);
}
