package com.drawai.trigger.chat.stream;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of currently active leader draw streams.
 */
@Component
public class DrawStreamRegistry {

    private final ConcurrentHashMap<String, ActiveDrawStream> streams = new ConcurrentHashMap<>();

    public ActiveDrawStream start(String sessionId, SseEmitter emitter) {
        ActiveDrawStream stream = new ActiveDrawStream(sessionId, emitter, this::remove);
        ActiveDrawStream old = streams.put(sessionId, stream);
        if (old != null) {
            old.cancelAndComplete("cancelled");
        }
        return stream;
    }

    public boolean cancel(String sessionId) {
        ActiveDrawStream stream = streams.remove(sessionId);
        if (stream == null) {
            return false;
        }
        stream.cancelAndComplete("cancelled");
        return true;
    }

    private void remove(String sessionId, ActiveDrawStream stream) {
        streams.remove(sessionId, stream);
    }
}
