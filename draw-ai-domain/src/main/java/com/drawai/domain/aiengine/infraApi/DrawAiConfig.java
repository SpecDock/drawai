package com.drawai.domain.aiengine.infraApi;

import com.drawai.domain.aiengine.model.ChatModelConfig;

/**
 * @author specdock
 * @Date 2026/6/4
 * @Time 18:09
 */


public interface DrawAiConfig {
    ChatModelConfig getChatModelConfigByRoleName(String roleName);
}
