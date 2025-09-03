package com.trend_now.backend.client;

import com.trend_now.backend.client.dto.NaverNewsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class NaverApiClient {
    private final WebClient.Builder webClientBuilder;

    private static final String NEWS_API_URL = "https://openapi.naver.com/v1/search/news.json";

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    public NaverNewsResponseDto searchNewsByKeyword(String keyword, int displayCount) {
        WebClient webClient = webClientBuilder.baseUrl(NEWS_API_URL).build();
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("query", keyword)
                .queryParam("display", displayCount)
                .build())
            .header("X-Naver-Client-Id", clientId)
            .header("X-Naver-Client-Secret", clientSecret)
            .retrieve()
            .bodyToMono(NaverNewsResponseDto.class).block();
    }

}
