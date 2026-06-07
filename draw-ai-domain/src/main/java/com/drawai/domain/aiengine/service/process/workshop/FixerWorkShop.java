package com.drawai.domain.aiengine.service.process.workshop;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.agent.StreamingChatModelContext;
import com.drawai.domain.aiengine.service.process.graph.GraphEventValidator;
import com.drawai.domain.aiengine.service.process.graph.GraphEventValidator.GraphEvent;
import com.drawai.domain.aiengine.types.RoleNameTypes;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Fixes a selected graph node or edge while preserving its original id.
 *
 * @author specdock
 * @Date 2026/6/6
 * @Time 18:08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FixerWorkShop {

    private final StreamingChatModelContext streamingChatModelContext;
    private final WorkerWorkShop workerWorkShop;
    private final GraphEventValidator graphEventValidator;

    public void workStream(String sessionId, String userFixMessage,
                           String selectedGraphEventJson, Consumer<GraphNodeDelta> sink) {
        GraphEvent selectedEvent;
        try {
            selectedEvent = graphEventValidator.parseSelected(selectedGraphEventJson);
        } catch (IllegalArgumentException e) {
            log.warn("invalid selected graph event for session={}: {}", sessionId, e.getMessage());
            sink.accept(GraphNodeDelta.fail(sessionId, "Selected graph event is invalid"));
            return;
        }

        UserMessage userMessage = UserMessage.from(buildFixerPrompt(userFixMessage, selectedGraphEventJson, selectedEvent));

        ChatRequest request = ChatRequest.builder()
                .messages(messagesWithInstruction(RoleNameTypes.FIXER.value(), List.of(userMessage)))
                .build();

        StreamingChatModel fixerChatModel = streamingChatModelContext.getModel(RoleNameTypes.FIXER.value());
        StringBuilder fixedAdvice = new StringBuilder();

        fixerChatModel.chat(request, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                fixedAdvice.append(partialResponse);
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                String completeAdvice = ThinkBlockFilter.strip(fixedAdvice.toString());
                workerWorkShop.workStream(sessionId, completeAdvice, idLockingSink(sessionId, selectedEvent, sink));
            }

            @Override
            public void onError(Throwable error) {
                log.error("fixer stream error for session={}", sessionId, error);
                sink.accept(GraphNodeDelta.fail(sessionId, "AI fixer stream failed"));
            }
        });
    }

    private Consumer<GraphNodeDelta> idLockingSink(String sessionId, GraphEvent selectedEvent,
                                                   Consumer<GraphNodeDelta> sink) {
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicBoolean emitted = new AtomicBoolean(false);

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
                if (emitted.get()) {
                    sink.accept(delta);
                } else {
                    sink.accept(GraphNodeDelta.fail(sessionId, "AI generated no fixed graph event"));
                }
                return;
            }

            if (!emitted.compareAndSet(false, true)) {
                closed.set(true);
                sink.accept(GraphNodeDelta.fail(sessionId, "AI generated more than one fixed graph event"));
                return;
            }

            try {
                GraphEvent event = graphEventValidator.parse(delta.content());
                if (!selectedEvent.type().equals(event.type())) {
                    throw new IllegalArgumentException("fixed event type changed");
                }
                GraphEvent locked = graphEventValidator.lockId(event, selectedEvent.id());
                String normalized = graphEventValidator.toJson(locked);
                sink.accept(GraphNodeDelta.delta(sessionId, normalized));
            } catch (IllegalArgumentException e) {
                log.warn("invalid fixer graph event for session={}: {}", sessionId, e.getMessage());
                closed.set(true);
                sink.accept(GraphNodeDelta.fail(sessionId, "AI generated an invalid fixed graph event"));
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

    private String buildFixerPrompt(String userFixMessage, String selectedGraphEventJson, GraphEvent selectedEvent) {
        return "请根据用户批注为下面这个 maxGraph " + selectedEvent.type() + " 生成修复建议。\n"
                + "后续 worker 会把你的建议转换成一个同类型 JSON 事件。\n"
                + "必须强调原始 id 不变: " + selectedEvent.id() + "。\n"
                + "用户批注:\n" + userFixMessage + "\n"
                + "原始数据:\n" + selectedGraphEventJson;
    }
}
