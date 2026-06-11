package com.drawai.trigger.chat;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.AiStreamingChatModelEngine;
import com.drawai.domain.aiengine.service.process.graph.GraphEventValidator;
import com.drawai.trigger.dto.ChatChunk;
import com.drawai.trigger.dto.ChatRequest;
import com.drawai.trigger.dto.DrawFixRequest;
import com.drawai.trigger.dto.DrawFixStreamCancelRequest;
import com.drawai.trigger.dto.DrawFixStreamCancelResponse;
import com.drawai.trigger.dto.DrawStreamCancelRequest;
import com.drawai.trigger.dto.DrawStreamCancelResponse;
import com.drawai.trigger.dto.Result;
import com.drawai.trigger.chat.stream.ActiveFixStream;
import com.drawai.trigger.chat.stream.ActiveDrawStream;
import com.drawai.trigger.chat.stream.DrawStreamRegistry;
import com.drawai.trigger.chat.stream.FixStreamRegistry;
import jakarta.annotation.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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

    @Resource
    private DrawStreamRegistry drawStreamRegistry;

    @Resource
    private FixStreamRegistry fixStreamRegistry;

    @Resource
    private GraphEventValidator graphEventValidator;

    @Resource(name = "applicationTaskExecutor")
    private TaskExecutor taskExecutor;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        ActiveDrawStream stream = drawStreamRegistry.start(req.sessionId(), emitter);
        emitter.onCompletion(stream::clientClosed);
        emitter.onTimeout(stream::clientClosed);
        emitter.onError(error -> stream.clientClosed());
        taskExecutor.execute(() -> {
            try {
                aiStreamingChatModelEngine.leaderStream(req.sessionId(), req.message(),
                        stream::send, stream.cancellation());
            } catch (Exception e) {
                stream.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/stream/cancel")
    public Result<DrawStreamCancelResponse> cancelStream(@RequestBody DrawStreamCancelRequest req) {
        boolean cancelled = drawStreamRegistry.cancel(req.sessionId());
        return Result.ok(new DrawStreamCancelResponse(req.sessionId(), cancelled));
    }

    @PostMapping(value = "/fix/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter fixStream(@RequestBody DrawFixRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        String targetId;
        try {
            targetId = graphEventValidator.parseSelected(req.selectedGraphEventJson()).id();
        } catch (IllegalArgumentException e) {
            taskExecutor.execute(() -> send(emitter,
                    toChunk(GraphNodeDelta.fail(req.sessionId(), "Selected graph event is invalid"))));
            return emitter;
        }

        ActiveFixStream stream = fixStreamRegistry.start(req.sessionId(), targetId, emitter);
        emitter.onCompletion(stream::clientClosed);
        emitter.onTimeout(stream::clientClosed);
        emitter.onError(error -> stream.clientClosed());
        taskExecutor.execute(() -> {
            try {
                aiStreamingChatModelEngine.fixerStream(req.sessionId(), req.message(), req.selectedGraphEventJson(),
                        stream::send, stream.cancellation());
            } catch (Exception e) {
                stream.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/fix/stream/cancel")
    public Result<DrawFixStreamCancelResponse> cancelFixStream(@RequestBody DrawFixStreamCancelRequest req) {
        boolean cancelled = fixStreamRegistry.cancel(req.sessionId(), req.targetId());
        return Result.ok(new DrawFixStreamCancelResponse(req.sessionId(), req.targetId(), cancelled));
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
