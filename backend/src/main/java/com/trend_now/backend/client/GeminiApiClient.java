package com.trend_now.backend.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Tool;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiClient {

    private final Client client;

    public GeminiApiClient(@Value("${google.gemini.api.key}") String apiKey) {
        this.client = Client.builder()
            .apiKey(apiKey).build();
    }

    public String generateAnswer(String prompt, String model) {
        GenerateContentResponse response =
            client.models.generateContent(
                model,
                prompt,
                null
            );
        return response.text();
    }

    /**
     * Google Search 옵션 활성화 설정
     */
    private GenerateContentConfig googleSearchTool() {
        GoogleSearch googleSearch = GoogleSearch.builder().build();
        Tool tool = Tool.builder()
            .googleSearch(googleSearch)
            .build();
        return GenerateContentConfig.builder()
            .tools(tool).build();
    }
}
