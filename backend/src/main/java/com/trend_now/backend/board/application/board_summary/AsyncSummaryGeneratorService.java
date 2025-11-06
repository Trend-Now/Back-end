package com.trend_now.backend.board.application.board_summary;

import com.trend_now.backend.client.GeminiApiClient;
import com.trend_now.backend.client.NaverApiClient;
import com.trend_now.backend.client.dto.NaverNewsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncSummaryGeneratorService {

    private static final int NAVER_NEWS_DISPLAY_COUNT = 10;
    // 네이버 뉴스 기사 기반 요약 요청 프롬프트
    private static final String SUMMARIZE_REQUEST_WITH_NEWS_PROMPT = """
        # 역할
        당신은 여러 뉴스를 종합하여 핵심 이슈를 객관적으로 분석하는 전문 뉴스 애널리스트입니다.
        
        # 목표
        분석할 키워드 = '%s'
        아래 5개의 뉴스 기사를 바탕으로, '[분석할 키워드]'에 대한 핵심 이슈를 3~4개의 완결된 문장으로 요약해 주세요.
        
        # 규칙
        -   **"제공된 뉴스 기사에 따르면", "뉴스 기사를 분석한 결과" 등과 같은 서술적 표현은 생략하고, 바로 핵심 내용만 기술하세요.**
        -   분석 과정이나 방법론을 언급하지 말고, 오직 핵심 사실만 기술하세요.
        -   반드시 제공된 기사 내용에만 근거하여 작성하세요.
        -   객관적이고 중립적인 사실만 전달하세요.
        -   당신의 의견을 추가하지 마세요.
        -   높임말을 사용하세요.
        -   전체 기사들 중, 대다수가 다루는 핵심 주제와 거리가 먼 소수의 기사가 있다면, 해당 기사는 요약 내용에 포함하지 말고 무시하세요.
        
        # 입력 데이터
        뉴스 기사 5개: '%s'
        """;
    // 구글 검색 기반 요약 요청 프롬프트
    private static final String SUMMARIZE_REQUEST_WITH_GOOGLE_SEARCH_PROMPT = """
        # 역할
        당신은 여러 뉴스를 종합하여 핵심 이슈를 객관적으로 분석하는 전문 뉴스 애널리스트입니다.
        
        # 목표
        분석할 키워드 = '%s'
        구글 검색을 통해 해당 키워드에 대한 내용을 파악한 후, '[분석할 키워드]'에 대한 핵심 이슈를 3~4개의 완결된 문장으로 요약해 주세요.
        
        # 규칙
        -   **"검색을 진행한 결과", "검색된 정보에 따르면" 등과 같은 서술적 표현은 생략하고, 바로 핵심 내용만 기술하세요.**
        -   분석 과정이나 방법론을 언급하지 말고, 오직 핵심 사실만 기술하세요.
        -   반드시 제공된 기사 내용에만 근거하여 작성하세요.
        -   객관적이고 중립적인 사실만 전달하세요.
        -   당신의 의견을 추가하지 마세요.
        -   높임말을 사용하세요.
        """;
    private static final String GEMINI_MODEL_NAME = "gemini-2.5-flash";
    // 네이버 뉴스 API에서 한 번에 가져올 뉴스 기사 수
    private static final String SUMMARY_SAVE_ERROR = "{}: 게시판 요약 저장에 실패했습니다. -> {}";
    private static final String COMPLETE_GENERATION_SUMMARY = "{}: 게시판 요약 생성이 완료되었습니다. -> {}";
    private static final String START_GENERATION_SUMMARY = "{}: 게시판 요약 생성이 시작되었습니다. -> {}";

    private final NaverApiClient naverApiClient;
    private final GeminiApiClient geminiApiClient;
    private final BoardSummaryService boardSummaryService;

    @Async
    public void generateSummaryAndSave(Long boardId, String keyword) {
        log.info(START_GENERATION_SUMMARY, getClass().getName(), keyword);
        // 키워드 관련 네이버 뉴스 기사 5개 조회
        NaverNewsResponseDto naverNewsResponseDto = naverApiClient.searchNewsByKeyword(keyword,
            NAVER_NEWS_DISPLAY_COUNT);

        // TODO: Gemini API가 응답하지 않는 경우에 대한 예외 처리 필요

        // 프롬프트 작성
        // 뉴스 기사가 없으면 자체 검색 모델로 요약, 있으면 일반 모델로 제공된 기사 바탕 요약
        String summary = naverNewsResponseDto.getItems().isEmpty()
            ? geminiApiClient.generateAnswerWithGoogleSearch(
            String.format(SUMMARIZE_REQUEST_WITH_GOOGLE_SEARCH_PROMPT,
                keyword), GEMINI_MODEL_NAME)
            : geminiApiClient.generateAnswer(
                String.format(SUMMARIZE_REQUEST_WITH_NEWS_PROMPT,
                    keyword, naverNewsResponseDto.getItems()), GEMINI_MODEL_NAME);

        log.info(COMPLETE_GENERATION_SUMMARY, getClass().getName(), keyword);

        // 게시판 요약 저장 또는 업데이트
        boardSummaryService.boardSummarySaveOrUpdate(boardId, summary);
    }
}
