package com.drawai.domain.aiengine.service.agent;

import com.drawai.domain.aiengine.infraApi.DrawAiConfig;
import com.drawai.domain.aiengine.model.ChatModelConfig;
import com.drawai.domain.aiengine.service.agent.impl.FixerStreamingChatModelBuider;
import com.drawai.domain.aiengine.service.agent.impl.LeaderStreamingChatModelBuilder;
import com.drawai.domain.aiengine.service.agent.impl.WorkerStreamingChatModelBuilder;
import com.drawai.domain.aiengine.types.RoleNameTypes;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the per-role ChatModels and registers them into {@link StreamingChatModelContext}.
 * Builds the models in {@code @PostConstruct} so dependencies are fully wired first.
 */
@Service
@RequiredArgsConstructor
public class StreamingChatModelFactory {

    private final WorkerStreamingChatModelBuilder workerBuilder;
    private final LeaderStreamingChatModelBuilder leaderBuilder;
    private final FixerStreamingChatModelBuider fixerBuilder;
    private final StreamingChatModelContext context;
    private final DrawAiConfig drawAiConfig;

    @PostConstruct
    void init() {
        Map<String, StreamingChatModel> models = new ConcurrentHashMap<>();
        models.put("worker", workerBuilder.build());
        models.put("leader", leaderBuilder.build());
        models.put("fixer", fixerBuilder.build());
        context.setModels(models);

        Map<String, String> instructions = new ConcurrentHashMap<>();
        putInstruction(instructions, RoleNameTypes.WORKER.value());
        putInstruction(instructions, RoleNameTypes.LEADER.value());
        putInstruction(instructions, RoleNameTypes.FIXER.value());
        context.setInstructions(instructions);
    }

    private void putInstruction(Map<String, String> instructions, String roleName) {
        ChatModelConfig cfg = drawAiConfig.getChatModelConfigByRoleName(roleName);
        if (cfg != null && cfg.getInstruction() != null && !cfg.getInstruction().isBlank()) {
            instructions.put(roleName, cfg.getInstruction());
        }
    }
}
