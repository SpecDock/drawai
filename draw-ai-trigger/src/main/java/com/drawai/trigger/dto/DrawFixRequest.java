package com.drawai.trigger.dto;

public record DrawFixRequest(String sessionId, String message, String selectedGraphEventJson) {
}
