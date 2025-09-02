package com.trend_now.backend.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.trend_now.backend.client.dto.NaverNewsResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiClient {

    public static final String SUMMARIZE_REQUEST_PROMPT = """
        ' %s '는 현재 대한민국 실시간 인기 검색어입니다. 아래 최근 뉴스 기사들을 참고하여, ' %s '가 왜 인기 검색어에 올랐는지 5문장 이내로 요약해 주세요.
        뉴스 기사: %s
        """;
    public static final String GEMINI_MODEL_NAME = "gemini-2.5-flash-lite";

    private final Client client;
    private final NaverApiClient naverApiClient;

    public GeminiApiClient(@Value("${google.gemini.api.key}") String apiKey, NaverApiClient naverApiClient) {
        this.client = Client.builder()
            .apiKey(apiKey).build();
        this.naverApiClient = naverApiClient;

    }

    public String summarizeKeyword(String keyword) {
        NaverNewsResponseDto naverNewsResponseDto = naverApiClient.searchNewsByKeyword(keyword);
        String prompt = String.format(SUMMARIZE_REQUEST_PROMPT, keyword, keyword, naverNewsResponseDto.getItems());
        GenerateContentResponse response =
            client.models.generateContent(
                GEMINI_MODEL_NAME,
                prompt,
                null);

        return response.text();
    }
}
