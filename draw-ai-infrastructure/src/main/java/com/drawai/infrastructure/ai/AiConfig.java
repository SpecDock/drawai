package com.drawai.infrastructure.ai;

import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author 29287
 */
@Configuration
public class AiConfig {

    @Bean
    public dev.langchain4j.model.chat.StreamingChatModel streamingChatModel(
            @Value("${ai.openai.base-url}") String baseUrl,
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model-name}") String modelName,
            @Value("${ai.openai.temperature:0.7}") Double temperature,
            @Value("${ai.openai.read-timeout-ms:120000}") Integer readMs
    ) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofMillis(readMs))
                .build();
    }
}
