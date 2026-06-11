package com.drawai.trigger.chat.stream;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of currently active chat streams.
 */
@Component
public class ChatStreamRegistry {

    private final ConcurrentHashMap<String, ActiveChatStream> streams = new ConcurrentHashMap<>();

    public ActiveChatStream start(String sessionId, SseEmitter emitter) {
        ActiveChatStream stream = new ActiveChatStream(sessionId, emitter, this::remove);
        ActiveChatStream old = streams.put(sessionId, stream);
        if (old != null) {
            old.cancelAndComplete("cancelled");
        }
        return stream;
    }

    public boolean cancel(String sessionId) {
        ActiveChatStream stream = streams.remove(sessionId);
        if (stream == null) {
            return false;
        }
        stream.cancelAndComplete("cancelled");
        return true;
    }

    private void remove(String sessionId, ActiveChatStream stream) {
        streams.remove(sessionId, stream);
    }
}
