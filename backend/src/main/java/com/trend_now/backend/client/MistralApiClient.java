package com.trend_now.backend.client;

import com.trend_now.backend.client.dto.NaverNewsResponseDto.NewsItem;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import java.util.List;

@AiService
public interface MistralApiClient {

    @SystemMessage("당신은 여러 뉴스를 종합하여 핵심 이슈를 객관적으로 분석하는 전문 뉴스 애널리스트입니다.")
    @UserMessage("""
        # 목표
        분석할 키워드 = '{{keyword}}'
        아래 5개의 뉴스 기사를 바탕으로, '[{{keyword}}]'에 대한 핵심 이슈를 3~4개의 완결된 문장으로 요약해 주세요.
        
        # 규칙
        - "제공된 뉴스 기사에 따르면", "뉴스 기사를 분석한 결과" 등과 같은 서술적 표현은 생략하고, 바로 핵심 내용만 기술하세요.
        - 분석 과정이나 방법론을 언급하지 말고, 오직 핵심 사실만 기술하세요.
        - 반드시 제공된 기사 내용에만 근거하여 작성하세요.
        - 객관적이고 중립적인 사실만 전달하세요.
        - 당신의 의견을 추가하지 마세요.
        - 높임말을 사용하세요.
        - 전체 기사들 중, 대다수가 다루는 핵심 주제와 거리가 먼 소수의 기사가 있다면, 해당 기사는 요약 내용에 포함하지 말고 무시하세요.
        
        # 입력 데이터
        뉴스 기사 5개:
        {{articles}}
        """)
    String analyzeNews(@V("keyword") String keyword, @V("articles") List<NewsItem> articles);

    String chat(String message);

}
