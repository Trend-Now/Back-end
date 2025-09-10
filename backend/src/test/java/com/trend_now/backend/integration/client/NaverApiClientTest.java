package com.trend_now.backend.integration.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.trend_now.backend.client.NaverApiClient;
import com.trend_now.backend.client.dto.NaverNewsResponseDto;
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
class NaverApiClientTest {

    @Autowired
    private NaverApiClient naverApiClient;

    @Test
    void 네이버_뉴스_검색_테스트() {
        // given
        String keyword = "네이버";
        int displayCount = 5;

        // when
        NaverNewsResponseDto response = naverApiClient.searchNewsByKeyword(keyword, displayCount);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getItems().size()).isEqualTo(displayCount);
    }
}