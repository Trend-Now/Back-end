package com.trend_now.backend.board.application.board_summary;

import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardSummaryService {

    private static final String BOARD_NOT_FOUND_MESSAGE = "해당 게시판이 존재하지 않습니다";
    private final BoardRepository boardRepository;
    private final BoardSummaryRepository boardSummaryRepository;

    @Transactional
    public void boardSummarySaveOrUpdate(Long boardId, String newSummary) {
        Boards boards = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE));

        BoardSummary boardSummary = boardSummaryRepository.findByBoards_Id(boardId)
            .orElseGet(() -> BoardSummary.builder()
                .boards(boards)
                .build());

        boardSummary.updateSummary(newSummary);
        boardSummaryRepository.save(boardSummary);
    }

}
