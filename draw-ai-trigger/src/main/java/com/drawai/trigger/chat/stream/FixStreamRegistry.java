package com.drawai.trigger.chat.stream;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of currently active fixer streams.
 */
@Component
public class FixStreamRegistry {

    private final ConcurrentHashMap<FixStreamKey, ActiveFixStream> streams = new ConcurrentHashMap<>();

    public ActiveFixStream start(String sessionId, String targetId, SseEmitter emitter) {
        FixStreamKey key = new FixStreamKey(sessionId, targetId);
        ActiveFixStream stream = new ActiveFixStream(key, emitter, this::remove);
        ActiveFixStream old = streams.put(key, stream);
        if (old != null) {
            old.cancelAndComplete("cancelled");
        }
        return stream;
    }

    public boolean cancel(String sessionId, String targetId) {
        ActiveFixStream stream = streams.remove(new FixStreamKey(sessionId, targetId));
        if (stream == null) {
            return false;
        }
        stream.cancelAndComplete("cancelled");
        return true;
    }

    private void remove(FixStreamKey key, ActiveFixStream stream) {
        streams.remove(key, stream);
    }
}
