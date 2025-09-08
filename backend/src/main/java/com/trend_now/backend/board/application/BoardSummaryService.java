package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.RankChangeType;
import com.trend_now.backend.board.repository.BoardSummaryRepository;
import com.trend_now.backend.client.GeminiApiClient;
import com.trend_now.backend.client.NaverApiClient;
import com.trend_now.backend.client.dto.NaverNewsResponseDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardSummaryService {

    private static final String SUMMARIZE_REQUEST_PROMPT = """
        # 역할
        당신은 여러 뉴스를 종합하여 핵심 이슈를 객관적으로 분석하는 전문 뉴스 애널리스트입니다.
        
        # 목표
        분석할 키워드 = '%s'
        아래 10개의 뉴스 기사를 바탕으로, '[분석할 키워드]'에 대한 핵심 이슈를 3~4개의 완결된 문장으로 요약해 주세요.
        
        # 규칙
        -   반드시 제공된 기사 내용에만 근거하여 작성하세요.
        -   객관적이고 중립적인 사실만 전달하세요.
        -   당신의 의견을 추가하지 마세요.
        -   높임말을 사용하세요.
        -   **전체 기사들 중, 대다수가 다루는 핵심 주제와 거리가 먼 소수의 기사가 있다면, 해당 기사는 요약 내용에 포함하지 말고 무시하세요.**
        
        
        # 입력 데이터
        뉴스 기사 10개: '%s'
        """;
    private static final String GEMINI_MODEL_NAME = "gemini-2.5-flash";
    // 네이버 뉴스 API에서 한 번에 가져올 뉴스 기사 수
    private static final int NAVER_NEWS_DISPLAY_COUNT = 5;

    private final NaverApiClient naverApiClient;
    private final GeminiApiClient geminiApiClient;
    private final BoardSummaryRepository boardSummaryRepository;

    public String summarizeKeyword(String keyword) {
        // 키워드 관련 네이버 뉴스 기사 10개 조회
        NaverNewsResponseDto naverNewsResponseDto = naverApiClient.searchNewsByKeyword(keyword, NAVER_NEWS_DISPLAY_COUNT);

        // 프롬프트 작성
        String prompt = String.format(SUMMARIZE_REQUEST_PROMPT, keyword, naverNewsResponseDto.getItems());

        // 응답 요청
        return geminiApiClient.generateAnswer(prompt, GEMINI_MODEL_NAME);

    }

    @Transactional
    public void saveOrUpdateBoardSummary(Boards boards, RankChangeType state) {
        Optional<BoardSummary> boardSummary = boardSummaryRepository.findByBoards(boards);

        // 새로 등재된 게시판(state = n)의 게시판 요약이 있다면 해당 내용을 업데이트
        if (boardSummary.isPresent() && state == RankChangeType.NEW) {
            String summary = summarizeKeyword(boards.getName());
            boardSummary.get().updateSummary(summary);
        // state 값과 상관 없이 게시판 요약이 없다면 새로 생성
        } else if (boardSummary.isEmpty()) {
            String summary = summarizeKeyword(boards.getName());
            BoardSummary newBoardSummary = BoardSummary.builder()
                .summary(summary)
                .boards(boards)
                .build();
            boardSummaryRepository.save(newBoardSummary);
        }
    }
}
