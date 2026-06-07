package com.drawai.domain.aiengine.model;

public record GraphNodeDelta(String sessionId, String content, boolean done, String error) {

    public static GraphNodeDelta delta(String sid, String c) {
        return new GraphNodeDelta(sid, c, false, null);
    }

    public static GraphNodeDelta end(String sid) {
        return new GraphNodeDelta(sid, "", true, null);
    }

    public static GraphNodeDelta fail(String sid, String e) {
        return new GraphNodeDelta(sid, "", true, e);
    }
}