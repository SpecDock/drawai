package com.drawai.domain.aiengine.service;

import com.drawai.domain.aiengine.model.GraphNodeDelta;

import java.util.function.Consumer;

/**
 * @author specdock
 * @Date 2026/6/4
 * @Time 21:42
 */
public interface AiStreamingChatModelEngine {

    void leaderStream(String sessionId, String userMessage, Consumer<GraphNodeDelta> sink);

    void fixerStream(String sessionId, String userFixMessage,
                     String selectedGraphEventJson, Consumer<GraphNodeDelta> sink);

}
