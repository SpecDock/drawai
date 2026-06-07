package com.drawai.infrastructure.ai;

import com.drawai.domain.aiengine.infraApi.DrawAiConfig;
import com.drawai.domain.aiengine.model.ChatModelConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author specdock
 * @Date 2026/6/4
 * @Time 18:03
 */

@Configuration
@ConfigurationProperties(prefix = "draw.ai")
@Data
public class DrawAiConfigProperties implements DrawAiConfig {

    private Map<String, ChatModelInfraConfig> models = new HashMap<>();

    @Override
    public ChatModelConfig getChatModelConfigByRoleName(String roleName) {

        ChatModelInfraConfig chatModelInfraConfig = models.get(roleName);
        if (chatModelInfraConfig == null){
            return null;
        }
        return new ChatModelConfig(
                chatModelInfraConfig.getRoleName(),
                chatModelInfraConfig.getBaseUrl(),
                chatModelInfraConfig.getApiKey(),
                chatModelInfraConfig.getModelName(),
                chatModelInfraConfig.getTemperature(),
                chatModelInfraConfig.getReadTimeoutMs(),
                chatModelInfraConfig.getInstruction()
        );
    }

    @Data
    private static class ChatModelInfraConfig {
        private String roleName;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Double temperature;
        private Integer readTimeoutMs;
        private String instruction;
    }

}
