package com.drawai.domain.aiengine.service.process;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.AiStreamingChatModelEngine;
import com.drawai.domain.aiengine.service.process.workshop.FixerWorkShop;
import com.drawai.domain.aiengine.service.process.workshop.LeaderWorkShop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * @author specdock
 * @Date 2026/6/6
 * @Time 16:20
 */
@Service
@RequiredArgsConstructor
public class EngineProcess implements AiStreamingChatModelEngine {

    private final LeaderWorkShop leaderWorkShop;
    private final FixerWorkShop fixerWorkShop;

    @Override
    public void leaderStream(String sessionId, String userMessage, Consumer<GraphNodeDelta> sink) {
        leaderWorkShop.workStream(sessionId, userMessage, sink);
    }

    @Override
    public void fixerStream(String sessionId, String userFixMessage,
                            String selectedGraphEventJson, Consumer<GraphNodeDelta> sink) {
        fixerWorkShop.workStream(sessionId, userFixMessage, selectedGraphEventJson, sink);
    }
}
