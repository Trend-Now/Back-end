package com.trend_now.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private static final String SIGNAL_BZ_BASE_URL = "https://api.signal.bz";

    @Bean
    public WebClient.Builder webClient() {
        return WebClient.builder();
    }
}

