package com.drawai.domain.aiengine.service.agent;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the per-role ChatModels built at startup.
 * Other modules should retrieve models from here, not from the factory.
 */
@Service
public class StreamingChatModelContext {

    private final Map<String, StreamingChatModel> models = new ConcurrentHashMap<>();
    private final Map<String, String> instructions = new ConcurrentHashMap<>();

    public void setModels(Map<String, StreamingChatModel> models) {
        this.models.putAll(models);
    }

    public StreamingChatModel getModel(String roleName) {
        return models.get(roleName);
    }

    public void setInstructions(Map<String, String> instructions) {
        this.instructions.putAll(instructions);
    }

    public String getInstruction(String roleName) {
        return instructions.get(roleName);
    }
}
