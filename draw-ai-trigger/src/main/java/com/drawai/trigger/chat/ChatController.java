package com.drawai.trigger.chat;

import com.drawai.domain.chat.model.ChatDelta;
import com.drawai.domain.chat.service.ChatService;
import com.drawai.trigger.dto.ChatChunk;
import com.drawai.trigger.dto.ChatRequest;
import com.drawai.trigger.dto.Result;
import jakarta.annotation.Resource;
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
 * @author 29287
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;
    private final ExecutorService worker = Executors.newCachedThreadPool();



    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        worker.submit(() -> {
            try {
                chatService.stream(req.sessionId(), req.message(),
                        delta -> send(emitter, toChunk(delta)));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    // Adapter: domain event -> transport DTO
    private ChatChunk toChunk(ChatDelta delta) {
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
