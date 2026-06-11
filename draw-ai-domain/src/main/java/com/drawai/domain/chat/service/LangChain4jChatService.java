package com.drawai.domain.chat.service;

import com.drawai.domain.chat.model.ChatDelta;
import com.drawai.domain.chat.service.stream.ChatStreamCancellation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LangChain4jChatService implements ChatService {

    private final StreamingChatModel streamingChatModel;

    // Simple per-session memory; size-bounded to keep memory tight
    private final ConcurrentHashMap<String, ChatMemory> memories = new ConcurrentHashMap<>();

    private ChatMemory memoryFor(String sessionId) {
        return memories.computeIfAbsent(sessionId,
                sid -> MessageWindowChatMemory.withMaxMessages(10));
    }

    @Override
    public void stream(String sessionId, String userMessage, Consumer<ChatDelta> sink) {
        stream(sessionId, userMessage, sink, new ChatStreamCancellation());
    }

    @Override
    public void stream(String sessionId, String userMessage,
                       Consumer<ChatDelta> sink, ChatStreamCancellation cancellation) {
        ChatMemory memory = memoryFor(sessionId);
        UserMessage userMessageObject = UserMessage.from(userMessage);
        List<ChatMessage> promptMessages;
        synchronized (memory) {
            promptMessages = new ArrayList<>(memory.messages());
        }
        promptMessages.add(userMessageObject);

        ChatRequest request = ChatRequest.builder()
                .messages(promptMessages)
                .build();

        streamingChatModel.chat(request, new StreamingChatResponseHandler() {

            private final AtomicBoolean handleBound = new AtomicBoolean(false);

            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                if (handleBound.compareAndSet(false, true)) {
                    cancellation.bind(context.streamingHandle());
                }
                if (cancellation.isCancelled()) {
                    context.streamingHandle().cancel();
                    return;
                }
                handlePartialText(partialResponse.text());
            }

            @Override
            public void onPartialResponse(String partialResponse) {
                handlePartialText(partialResponse);
            }

            private void handlePartialText(String text) {
                if (cancellation.isCancelled() || text == null || text.isEmpty()) {
                    return;
                }
                sink.accept(ChatDelta.delta(sessionId, text));
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (cancellation.isCancelled()) {
                    return;
                }
                AiMessage ai = completeResponse.aiMessage();
                synchronized (memory) {
                    if (cancellation.isCancelled()) {
                        return;
                    }
                    memory.add(userMessageObject);
                    memory.add(ai);
                }
                sink.accept(ChatDelta.end(sessionId));
            }

            @Override
            public void onError(Throwable error) {
                if (cancellation.isCancelled()) {
                    return;
                }
                log.error("chat stream error for session={}", sessionId, error);
                sink.accept(ChatDelta.fail(sessionId, error.getMessage()));
            }
        });
    }
}
