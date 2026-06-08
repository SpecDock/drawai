package com.drawai.domain.aiengine.service.process.workshop;

import com.drawai.domain.aiengine.model.GraphNodeDelta;
import com.drawai.domain.aiengine.service.agent.StreamingChatModelContext;
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
import java.util.function.Consumer;

/**
 * Converts drawing advice into raw graph event JSON streams.
 *
 * @author specdock
 * @Date 2026/6/6
 * @Time 18:08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerWorkShop {

    private final StreamingChatModelContext streamingChatModelContext;


    public void workStream(String sessionId, String drawingAdvice, Consumer<GraphNodeDelta> sink) {
        String cleanDrawingAdvice = ThinkBlockFilter.strip(drawingAdvice);
        ChatRequest request = ChatRequest.builder()
                .messages(messagesWithInstruction(cleanDrawingAdvice))
                .build();

        StreamingChatModel workerChatModel = streamingChatModelContext.getModel(RoleNameTypes.WORKER.value());
        StringBuilder objectBuffer = new StringBuilder();
        ThinkBlockFilter.Stream thinkFilter = ThinkBlockFilter.stream();

        workerChatModel.chat(request, new StreamingChatResponseHandler() {



            @Override
            public void onPartialResponse(String partialResponse) {
                emitCompletedObjects(sessionId, thinkFilter.append(partialResponse), objectBuffer, sink);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                emitCompletedObjects(sessionId, thinkFilter.finish(), objectBuffer, sink);
                if (!objectBuffer.toString().trim().isEmpty()) {
                    log.warn("worker stream ended with incomplete graph event for session={}", sessionId);
                    sink.accept(GraphNodeDelta.fail(sessionId, "AI output ended with an incomplete graph event"));
                    return;
                }
                sink.accept(GraphNodeDelta.end(sessionId));
            }

            @Override
            public void onError(Throwable error) {
                log.error("worker stream error for session={}", sessionId, error);
                sink.accept(GraphNodeDelta.fail(sessionId, "AI worker stream failed"));
            }
        });
    }

    private List<ChatMessage> messagesWithInstruction(String drawingAdvice) {
        List<ChatMessage> messages = new ArrayList<>();
        String instruction = streamingChatModelContext.getInstruction(RoleNameTypes.WORKER.value());
        if (instruction != null && !instruction.isBlank()) {
            messages.add(SystemMessage.from(instruction));
        }
        messages.add(UserMessage.from(drawingAdvice));
        return messages;
    }

    private void emitCompletedObjects(String sessionId, String partialResponse,
                                      StringBuilder objectBuffer, Consumer<GraphNodeDelta> sink) {
        objectBuffer.append(partialResponse);
        int objectEndIndex = findCompleteJsonObjectEnd(objectBuffer);
        while (objectEndIndex >= 0) {
            emitObject(sessionId, objectBuffer.substring(0, objectEndIndex), sink);
            objectBuffer.delete(0, objectEndIndex);
            objectEndIndex = findCompleteJsonObjectEnd(objectBuffer);
        }
    }

    private void emitObject(String sessionId, String jsonObject, Consumer<GraphNodeDelta> sink) {
        String content = jsonObject.trim();
        if (!content.isEmpty()) {
            sink.accept(GraphNodeDelta.delta(sessionId, content));
            log.info(content);

        }
    }

    private int findCompleteJsonObjectEnd(StringBuilder objectBuffer) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        boolean started = false;

        for (int i = 0; i < objectBuffer.length(); i++) {
            char ch = objectBuffer.charAt(i);
            if (!started) {
                if (Character.isWhitespace(ch)) {
                    continue;
                }
                if (ch != '{') {
                    return -1;
                }
                started = true;
                depth = 1;
                continue;
            }

            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = inString;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }

        return -1;
    }
}
