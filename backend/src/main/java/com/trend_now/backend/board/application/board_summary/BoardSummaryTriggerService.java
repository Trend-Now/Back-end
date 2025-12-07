package com.trend_now.backend.board.application.board_summary;

import com.trend_now.backend.board.dto.BoardKeyProvider;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSummaryTriggerService {

    private final BoardSummaryRepository boardSummaryRepository;
    private final AsyncSummaryGeneratorService asyncSummaryGeneratorService;
    private final BoardCache boardCache;
    private final RedisTemplate<String, String> redisTemplate;

    // Self-Invocation 문제 해결을 위해 별도의 서비스로 분리
    public void triggerSummaryUpdate(BoardKeyProvider boardKeyProvider) {
        // 게시판 요약이 존재하지 않거나, state 값이 NEW라면 요약 생성 작업 비동기 등록
        log.info(("{} 게시판의 요약 생성 작업을 비동기로 시작합니다."), boardKeyProvider.getBoardName());
        asyncSummaryGeneratorService.generateSummaryAndSave(boardKeyProvider.getBoardId(), boardKeyProvider.getBoardName());
    }
}
