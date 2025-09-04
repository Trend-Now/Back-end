package com.trend_now.backend.integration.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.client.GeminiApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class GeminiApiClientTest {

    @Autowired
    private GeminiApiClient geminiApiClient;

    @Test
    @DisplayName("실제 Gemini API를 호출하여 응답을 성공적으로 받아온다.")
    void GEMINI_키워드_생성_테스트() {
        // given
        String keyword = "GEMINI에 대한 한 줄 소개";
        String model = "gemini-2.5-flash-lite";

        // when
        String response = geminiApiClient.generateAnswer(keyword, model);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isNotBlank();
    }
}