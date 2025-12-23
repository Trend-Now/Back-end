package com.trend_now.backend.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MistralConfig {

    @Bean
    public ChatModel mistralChatModel(
        @Value("${mistral.api-key}") String apiKey,
        @Value("${mistral.model-name}") String modelName
    ) {
        return MistralAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .build();
    }
}
