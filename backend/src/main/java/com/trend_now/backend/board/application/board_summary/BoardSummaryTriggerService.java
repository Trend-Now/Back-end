package com.trend_now.backend.board.application.board_summary;

import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSummaryTriggerService {

    private final BoardSummaryRepository boardSummaryRepository;
    private final AsyncSummaryGeneratorService asyncSummaryGeneratorService;

    // Self-Invocation 문제 해결을 위해 별도의 서비스로 분리
    public void triggerSummaryUpdate(Long boardId, String keyword, RankChangeType state) {
        Optional<BoardSummary> optionalBoardSummary = boardSummaryRepository.findByBoards_Id(boardId);

        // 게시판 요약이 존재하고, state 값이 NEW가 아니라면 아무 작업도 수행하지 않음
        if (optionalBoardSummary.isPresent() && state != RankChangeType.NEW) {
            return;
        }

        // 게시판 요약이 존재하지 않거나, state 값이 NEW라면 요약 생성 작업 비동기 등록
        asyncSummaryGeneratorService.generateSummaryAndSave(boardId, keyword);
    }
}
