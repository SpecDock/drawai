package com.drawai.domain.aiengine.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record GraphNodeDelta(String sessionId, String content, boolean done, String error) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static GraphNodeDelta delta(String sid, String c) {
        return new GraphNodeDelta(sid, c, false, null);
    }

    public static GraphNodeDelta think(String sid, String source, String content) {
        return delta(sid, thinkJson(source, content));
    }

    public static GraphNodeDelta end(String sid) {
        return new GraphNodeDelta(sid, "", true, null);
    }

    public static GraphNodeDelta fail(String sid, String e) {
        return new GraphNodeDelta(sid, "", true, e);
    }

    private static String thinkJson(String source, String content) {
        ObjectNode data = OBJECT_MAPPER.createObjectNode();
        data.put("source", source == null || source.isBlank() ? "unknown" : source);
        data.put("content", content == null ? "" : content);

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("type", "think");
        root.set("data", data);
        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize think event", e);
        }
    }
}
