package com.drawai.trigger.chat.stream;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.process.stream.DrawStreamCancellation;
import com.drawai.trigger.dto.ChatChunk;
import com.drawai.trigger.dto.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime state for one active fixer SSE stream.
 */
public final class ActiveFixStream {

    private final FixStreamKey key;
    private final SseEmitter emitter;
    private final DrawStreamCancellation cancellation = new DrawStreamCancellation();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final FixStreamCleanup cleanup;
    private final Object sendLock = new Object();

    public ActiveFixStream(FixStreamKey key, SseEmitter emitter, FixStreamCleanup cleanup) {
        this.key = key;
        this.emitter = emitter;
        this.cleanup = cleanup;
    }

    public DrawStreamCancellation cancellation() {
        return cancellation;
    }

    public void send(GraphNodeDelta delta) {
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
        sendFinal(GraphNodeDelta.fail(key.sessionId(), reason));
    }

    public void clientClosed() {
        synchronized (sendLock) {
            if (completed.compareAndSet(false, true)) {
                cancellation.cancel();
                cleanup.run(key, this);
            }
        }
    }

    public void completeWithError(Throwable error) {
        synchronized (sendLock) {
            completeWithErrorLocked(error);
        }
    }

    private void sendFinal(GraphNodeDelta delta) {
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
                cleanup.run(key, this);
            }
        }
    }

    private void completeWithErrorLocked(Throwable error) {
        if (completed.compareAndSet(false, true)) {
            cancellation.cancel();
            try {
                emitter.completeWithError(error);
            } finally {
                cleanup.run(key, this);
            }
        }
    }

    private void sendChunk(ChatChunk chunk) throws IOException {
        emitter.send(SseEmitter.event()
                .name(chunk.done() ? "done" : "delta")
                .data(Result.ok(chunk)));
    }

    private ChatChunk toChunk(GraphNodeDelta delta) {
        return new ChatChunk(delta.sessionId(), delta.content(), delta.done(), delta.error());
    }

    @FunctionalInterface
    public interface FixStreamCleanup {
        void run(FixStreamKey key, ActiveFixStream stream);
    }
}
