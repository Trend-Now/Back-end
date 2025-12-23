package com.trend_now.backend.integration.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.client.MistralApiClient;
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
class MistralApiClientTest {

    @Autowired
    private MistralApiClient mistralApiClient;

    @Test
    void chat_실제_AI_호출_테스트() {
        // given
        String message = "Hello";

        // when
        String response = mistralApiClient.chat(message);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        System.out.println(response);
    }
}