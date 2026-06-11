package com.drawai.domain.chat.service.stream;

import dev.langchain4j.model.chat.response.StreamingHandle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cancellation token shared by the HTTP chat stream owner and AI callbacks.
 */
public final class ChatStreamCancellation {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<StreamingHandle> handleRef = new AtomicReference<>();

    public void bind(StreamingHandle handle) {
        if (handle == null) {
            return;
        }
        handleRef.set(handle);
        if (cancelled.get()) {
            handle.cancel();
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set(true);
        StreamingHandle handle = handleRef.get();
        if (handle != null) {
            handle.cancel();
        }
    }
}
