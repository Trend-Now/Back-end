package com.trend_now.backend.post.repository;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.post.domain.Posts;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostsRepository extends JpaRepository<Posts, Long> {

    Page<Posts> findAllByBoards_Id(Long boardsId, Pageable pageable);

    void deleteAllByMembers_Id(Long membersId);

    Page<Posts> findByMembers_Id(Long membersId, Pageable pageable);

    // 게시글 제목, 내용에 키워드가 포함된 게시글 중, 실시간 검색어 게시판에 속한 게시물 조회
    @Query("""
        SELECT p from Posts p
        WHERE (p.content LIKE %:keyword% OR p.title LIKE %:keyword%)
        AND p.boards.id IN :boardIds
        """)
    Page<Posts> findByKeywordAndRealTimeBoard(
        @Param("keyword") String keyword, @Param("boardIds") Set<Long> boardIds, Pageable pageable);

    @Query("""
        SELECT p FROM Posts p
                WHERE (p.content LIKE %:keyword% OR p.title LIKE %:keyword%)
                AND p.boards.boardCategory = :category
        """)
    Page<Posts> findByBoardCategoryAndKeyword(@Param("keyword") String keyword, @Param("category") BoardCategory category, Pageable pageable);
}
