package com.drawai.trigger.dto;

public record ChatChunk(String sessionId, String content, boolean done, String error) {

    public static ChatChunk delta(String sid, String c) {
        return new ChatChunk(sid, c, false, null);
    }

    public static ChatChunk end(String sid) {
        return new ChatChunk(sid, "", true, null);
    }

    public static ChatChunk fail(String sid, String e) {
        return new ChatChunk(sid, "", true, e);
    }
}
