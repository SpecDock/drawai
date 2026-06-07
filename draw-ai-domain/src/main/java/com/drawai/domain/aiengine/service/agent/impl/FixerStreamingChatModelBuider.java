package com.drawai.domain.aiengine.service.agent.impl;

import com.drawai.domain.aiengine.infraApi.DrawAiConfig;
import com.drawai.domain.aiengine.model.ChatModelConfig;
import com.drawai.domain.aiengine.types.RoleNameTypes;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Builds the ChatModel for the fixer role.
 * @author 29287
 */
@Service
@RequiredArgsConstructor
public class FixerStreamingChatModelBuider {

    private final DrawAiConfig drawAiConfig;

    public StreamingChatModel build() {
        ChatModelConfig cfg = drawAiConfig.getChatModelConfigByRoleName(RoleNameTypes.FIXER.value());
        return OpenAiStreamingChatModel.builder()
                .baseUrl(cfg.getBaseurl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getModelName())
                .temperature(cfg.getTemperature())
                .timeout(Duration.ofMillis(cfg.getReadTimeoutMs()))
                .build();
    }
}
