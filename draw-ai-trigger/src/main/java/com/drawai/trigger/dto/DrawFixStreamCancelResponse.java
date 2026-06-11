package com.drawai.trigger.dto;

public record DrawFixStreamCancelResponse(String sessionId, String targetId, boolean cancelled) {
}
