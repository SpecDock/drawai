package com.drawai.trigger.chat.stream;

import com.drawai.domain.chat.model.ChatDelta;
import com.drawai.domain.chat.service.stream.ChatStreamCancellation;
import com.drawai.trigger.dto.ChatChunk;
import com.drawai.trigger.dto.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime state for one active chat SSE stream.
 */
public final class ActiveChatStream {

    private final String sessionId;
    private final SseEmitter emitter;
    private final ChatStreamCancellation cancellation = new ChatStreamCancellation();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final ChatStreamCleanup cleanup;
    private final Object sendLock = new Object();

    public ActiveChatStream(String sessionId, SseEmitter emitter, ChatStreamCleanup cleanup) {
        this.sessionId = sessionId;
        this.emitter = emitter;
        this.cleanup = cleanup;
    }

    public ChatStreamCancellation cancellation() {
        return cancellation;
    }

    public void send(ChatDelta delta) {
        if (delta.done()) {
            sendFinal(delta);
            return;
        }
        synchronized (sendLock) {
            if (completed.get() || cancellation.isCancelled()) {
                return;
            }
            try {
                sendChunk(toChunk(delta));
            } catch (IOException e) {
                completeWithErrorLocked(e);
            }
        }
    }

    public void cancelAndComplete(String reason) {
        cancellation.cancel();
        sendFinal(ChatDelta.fail(sessionId, reason));
    }

    public void clientClosed() {
        synchronized (sendLock) {
            if (completed.compareAndSet(false, true)) {
                cancellation.cancel();
                cleanup.run(sessionId, this);
            }
        }
    }

    public void completeWithError(Throwable error) {
        synchronized (sendLock) {
            completeWithErrorLocked(error);
        }
    }

    private void sendFinal(ChatDelta delta) {
        synchronized (sendLock) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            if (delta.error() != null) {
                cancellation.cancel();
            }
            try {
                sendChunk(toChunk(delta));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } finally {
                cleanup.run(sessionId, this);
            }
        }
    }

    private void completeWithErrorLocked(Throwable error) {
        if (completed.compareAndSet(false, true)) {
            cancellation.cancel();
            try {
                emitter.completeWithError(error);
            } finally {
                cleanup.run(sessionId, this);
            }
        }
    }

    private void sendChunk(ChatChunk chunk) throws IOException {
        emitter.send(SseEmitter.event()
                .name(chunk.done() ? "done" : "delta")
                .data(Result.ok(chunk)));
    }

    private ChatChunk toChunk(ChatDelta delta) {
        return new ChatChunk(delta.sessionId(), delta.content(), delta.done(), delta.error());
    }

    @FunctionalInterface
    public interface ChatStreamCleanup {
        void run(String sessionId, ActiveChatStream stream);
    }
}
