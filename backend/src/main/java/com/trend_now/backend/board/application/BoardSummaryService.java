package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardSummary;
import com.trend_now.backend.board.domain.Boards;
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
        # Persona
        너는 최신 트렌드와 뉴스 기사를 분석하여 핵심 원인을 요약하는 전문 애널리스트야.
        
        # Instruction
        내가 제공하는 '키워드'와 '관련 뉴스기사'들을 바탕으로, 해당 키워드가 현재 왜 실시간 인기 검색어인지 그 이유를 분석해 줘.
        자연스러운 말투를 사용하고, 문어체보다는 구어체에 가깝게 작성해 줘.
        분석한 내용은 반드시 '한 줄 요약'과 '상세 요약'으로 구성해야 해.
        
        # Input Data
        * 키워드: %s
        * 관련 뉴스기사:
            ""\"
            %s
            ""\"
        
        # Output Format
        결과는 아래 형식을 반드시 준수해야 하며, '한 줄 요약'과 '상세 요약' 사이는 특수 문자 '||'로 구분해야 해. 다른 부연 설명은 일절 추가하지 마.
        
        한 줄 요약 결과||상세 요약 결과
        """;
    private static final String SUMMARY_DELIMITER = "\\|\\|";
    private static final String GEMINI_MODEL_NAME = "gemini-2.5-flash-lite";
    // 네이버 뉴스 API에서 한 번에 가져올 뉴스 기사 수
    private static final int NAVER_NEWS_DISPLAY_COUNT = 10;

    private final NaverApiClient naverApiClient;
    private final GeminiApiClient geminiApiClient;
    private final BoardSummaryRepository boardSummaryRepository;

    public String[] summarizeKeyword(String keyword) {
        // 키워드 관련 네이버 뉴스 기사 10개 조회
        NaverNewsResponseDto naverNewsResponseDto = naverApiClient.searchNewsByKeyword(keyword, NAVER_NEWS_DISPLAY_COUNT);

        // 프롬프트 작성
        String prompt = String.format(SUMMARIZE_REQUEST_PROMPT, keyword, naverNewsResponseDto.getItems());

        // 응답 요청
        String summary = geminiApiClient.generateAnswer(prompt, GEMINI_MODEL_NAME);

        String[] splitSummary = summary.split(SUMMARY_DELIMITER);
        if (splitSummary.length != 2) {
            throw new IllegalStateException("GEMINI API 응답 형식이 올바르지 않습니다: " + summary);
        }
        // 앞뒤 공백 제거
        splitSummary[0] = splitSummary[0].trim();
        splitSummary[1] = splitSummary[1].trim();
        return splitSummary;
    }

    @Transactional
    public void saveOrUpdateBoardSummary(Boards boards) {
        Optional<BoardSummary> boardSummary = boardSummaryRepository.findByBoards(boards);

        String[] splitSummary = summarizeKeyword(boards.getName());
        // 기존에 저장 돼 있던 게시판 요약이 있으면 업데이트, 없으면 새로 생성
        if (boardSummary.isPresent()) {
            boardSummary.get().updateSummary(splitSummary[0], splitSummary[1]);
        } else {
            BoardSummary newBoardSummary = BoardSummary.builder()
                .summary(splitSummary[0])
                .details(splitSummary[1])
                .boards(boards)
                .build();
            boardSummaryRepository.save(newBoardSummary);
        }
    }
}
