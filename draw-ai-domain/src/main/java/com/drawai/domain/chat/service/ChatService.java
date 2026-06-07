package com.drawai.domain.chat.service;

import com.drawai.domain.chat.model.ChatDelta;

import java.util.function.Consumer;

public interface ChatService {

    /**
     * Stream AI response chunks for the given session + message.
     * Implementations MUST be safe to call from request-handling threads
     * and MUST invoke the sink on the calling thread (controller's choice
     * whether to bridge to SseEmitter).
     */
    void stream(String sessionId, String userMessage, Consumer<ChatDelta> sink);
}
