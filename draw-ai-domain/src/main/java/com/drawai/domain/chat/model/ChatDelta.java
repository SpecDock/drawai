package com.drawai.domain.chat.model;

/**
 * Pure-domain streaming event for chat responses.
 * The transport layer (trigger) translates this into wire-format DTOs.
 */
public record ChatDelta(String sessionId, String content, boolean done, String error) {

    public static ChatDelta delta(String sid, String c) {
        return new ChatDelta(sid, c, false, null);
    }

    public static ChatDelta end(String sid) {
        return new ChatDelta(sid, "", true, null);
    }

    public static ChatDelta fail(String sid, String e) {
        return new ChatDelta(sid, "", true, e);
    }
}
