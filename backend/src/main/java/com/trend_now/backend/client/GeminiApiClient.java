package com.trend_now.backend.client;

import com.google.genai.Client;
//import com.google.genai.types.GenerateContentConfig;
//import com.google.genai.types.GoogleSearch;
//import com.google.genai.types.Tool;
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
        // Google Search API 연동 예시 (필요시 활성화)
//        GoogleSearch googleSearch = GoogleSearch.builder().build();
//        Tool tool = Tool.builder()
//            .googleSearch(googleSearch)
//            .build();
//        GenerateContentConfig config = GenerateContentConfig.builder()
//            .tools(tool).build();

        GenerateContentResponse response =
            client.models.generateContent(
                model,
                prompt,
                null
//                config
            );
        return response.text();
    }
}
