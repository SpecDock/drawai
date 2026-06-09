package com.drawai.domain.aiengine.service.process.workshop;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.agent.StreamingChatModelContext;
import com.drawai.domain.aiengine.service.process.graph.GraphEventValidator;
import com.drawai.domain.aiengine.types.RoleNameTypes;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Generates a full graph from user drawing descriptions.
 *
 * @author specdock
 * @Date 2026/6/6
 * @Time 18:08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaderWorkShop {

    private static final int MAX_SESSION_MEMORIES = 1000;

    private final StreamingChatModelContext streamingChatModelContext;
    private final WorkerWorkShop workerWorkShop;
    private final GraphEventValidator graphEventValidator;

    private final ConcurrentHashMap<String, ChatMemory> memories = new ConcurrentHashMap<>();

    private synchronized ChatMemory memoryFor(String sessionId) {
        if (!memories.containsKey(sessionId) && memories.size() >= MAX_SESSION_MEMORIES) {
            memories.keySet().stream().findFirst().ifPresent(memories::remove);
        }
        return memories.computeIfAbsent(sessionId,
                sid -> MessageWindowChatMemory.withMaxMessages(100));
    }

    public void workStream(String sessionId, String userMessage, Consumer<GraphNodeDelta> sink) {
        ChatMemory memory = memoryFor(sessionId);
        memory.add(UserMessage.from(userMessage));

        ChatRequest request = ChatRequest.builder()
                .messages(messagesWithInstruction(RoleNameTypes.LEADER.value(), memory.messages()))
                .build();

        StreamingChatModel leaderChatModel = streamingChatModelContext.getModel(RoleNameTypes.LEADER.value());
        StringBuilder drawingAdvice = new StringBuilder();

        leaderChatModel.chat(request, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                drawingAdvice.append(partialResponse);
                System.out.print(partialResponse);
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    sink.accept(GraphNodeDelta.think(sessionId, "leader", partialResponse));
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                String completeAdvice = ThinkBlockFilter.strip(drawingAdvice.toString());
                log.info("\n\n==========================================================\n{}",  completeAdvice);
                memory.add(AiMessage.from(completeAdvice));
                workerWorkShop.workStream(sessionId, completeAdvice,
                        validatingSink(sessionId, sink),
                        sink);
            }

            @Override
            public void onError(Throwable error) {
                log.error("leader stream error for session={}", sessionId, error);
                sink.accept(GraphNodeDelta.fail(sessionId, "AI leader stream failed"));
            }
        });
    }

    private Consumer<GraphNodeDelta> validatingSink(String sessionId, Consumer<GraphNodeDelta> sink) {
        AtomicBoolean closed = new AtomicBoolean(false);

        return delta -> {
            if (closed.get()) {
                return;
            }
            if (delta.error() != null) {
                closed.set(true);
                sink.accept(delta);
                return;
            }
            if (delta.done()) {
                closed.set(true);
                sink.accept(delta);
                return;
            }

            try {
                String normalized = graphEventValidator.normalize(delta.content());
                sink.accept(GraphNodeDelta.delta(sessionId, normalized));
            } catch (IllegalArgumentException e) {
                log.warn("invalid worker graph event for session={}: {}", sessionId, e.getMessage());
                closed.set(true);
                sink.accept(GraphNodeDelta.fail(sessionId, "AI generated an invalid graph event"));
            }
        };
    }

    private List<ChatMessage> messagesWithInstruction(String roleName, List<ChatMessage> memoryMessages) {
        List<ChatMessage> messages = new ArrayList<>();
        String instruction = streamingChatModelContext.getInstruction(roleName);
        if (instruction != null && !instruction.isBlank()) {
            messages.add(SystemMessage.from(instruction));
        }
        messages.addAll(memoryMessages);
        return messages;
    }
}
