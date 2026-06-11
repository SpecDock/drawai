package com.drawai.trigger.chat;

import com.drawai.domain.chat.service.ChatService;
import com.drawai.trigger.chat.stream.ActiveChatStream;
import com.drawai.trigger.chat.stream.ChatStreamRegistry;
import com.drawai.trigger.dto.ChatRequest;
import com.drawai.trigger.dto.ChatStreamCancelRequest;
import com.drawai.trigger.dto.ChatStreamCancelResponse;
import com.drawai.trigger.dto.Result;
import jakarta.annotation.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author 29287
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ChatStreamRegistry chatStreamRegistry;

    @Resource(name = "applicationTaskExecutor")
    private TaskExecutor taskExecutor;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        ActiveChatStream stream = chatStreamRegistry.start(req.sessionId(), emitter);
        emitter.onCompletion(stream::clientClosed);
        emitter.onTimeout(stream::clientClosed);
        emitter.onError(error -> stream.clientClosed());
        taskExecutor.execute(() -> {
            try {
                chatService.stream(req.sessionId(), req.message(),
                        stream::send, stream.cancellation());
            } catch (Exception e) {
                stream.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/stream/cancel")
    public Result<ChatStreamCancelResponse> cancelStream(@RequestBody ChatStreamCancelRequest req) {
        boolean cancelled = chatStreamRegistry.cancel(req.sessionId());
        return Result.ok(new ChatStreamCancelResponse(req.sessionId(), cancelled));
    }
}
