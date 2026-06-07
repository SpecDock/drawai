package com.drawai.domain.chat.service;

import com.drawai.domain.chat.model.ChatDelta;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
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
        ChatMemory memory = memoryFor(sessionId);
        memory.add(UserMessage.from(userMessage));
        ChatRequest request = ChatRequest.builder()
                .messages(memory.messages())
                .build();

        streamingChatModel.chat(request, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                sink.accept(ChatDelta.delta(sessionId, partialResponse));
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage ai = completeResponse.aiMessage();
                memory.add(ai);
                sink.accept(ChatDelta.end(sessionId));
            }

            @Override
            public void onError(Throwable error) {
                log.error("chat stream error for session={}", sessionId, error);
                sink.accept(ChatDelta.fail(sessionId, error.getMessage()));
            }
        });
    }
}
