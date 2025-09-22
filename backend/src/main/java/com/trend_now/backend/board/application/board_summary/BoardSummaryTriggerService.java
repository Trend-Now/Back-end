package com.trend_now.backend.board.application.board_summary;

import com.trend_now.backend.board.cache.BoardCache;
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
    private final BoardCache boardCache;

    // Self-Invocation 문제 해결을 위해 별도의 서비스로 분리
    public void triggerSummaryUpdate(Long boardId, String keyword) {
        Optional<BoardSummary> optionalBoardSummary = boardSummaryRepository.findByBoards_Id(boardId);
        boolean inBoardCache = boardCache.isInBoardCache(boardId);

        // 게시판 요약이 존재하고, 이전에 실시간 순위에 있던 게시판이라면 요약 생성 작업 무시
        if (optionalBoardSummary.isPresent() && inBoardCache) {
            return;
        }

        // 게시판 요약이 존재하지 않거나, state 값이 NEW라면 요약 생성 작업 비동기 등록
        asyncSummaryGeneratorService.generateSummaryAndSave(boardId, keyword);
    }
}
