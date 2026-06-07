package com.drawai.trigger.chat;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.AiStreamingChatModelEngine;
import com.drawai.trigger.dto.ChatChunk;
import com.drawai.trigger.dto.ChatRequest;
import com.drawai.trigger.dto.DrawFixRequest;
import com.drawai.trigger.dto.Result;
import jakarta.annotation.Resource;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Streams AI-generated graph nodes and edges.
 *
 * @author specdock
 */
@RestController
@RequestMapping("/api/draw")
public class DrawController {

    @Resource
    private AiStreamingChatModelEngine aiStreamingChatModelEngine;

    private final ExecutorService worker = Executors.newFixedThreadPool(16);

    @PreDestroy
    public void shutdown() {
        worker.shutdown();
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        worker.submit(() -> {
            try {
                aiStreamingChatModelEngine.leaderStream(req.sessionId(), req.message(),
                        delta -> send(emitter, toChunk(delta)));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }




    @PostMapping(value = "/fix/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter fixStream(@RequestBody DrawFixRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        worker.submit(() -> {
            try {
                aiStreamingChatModelEngine.fixerStream(req.sessionId(), req.message(), req.selectedGraphEventJson(),
                        delta -> send(emitter, toChunk(delta)));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private ChatChunk toChunk(GraphNodeDelta delta) {
        return new ChatChunk(delta.sessionId(), delta.content(), delta.done(), delta.error());
    }

    private void send(SseEmitter emitter, ChatChunk chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name(chunk.done() ? "done" : "delta")
                    .data(Result.ok(chunk)));
            if (chunk.done()) {
                emitter.complete();
            }
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
