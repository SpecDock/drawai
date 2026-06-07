package com.drawai.domain.aiengine.types;

import java.util.Arrays;

/**
 * Role identifiers used to look up ChatModelConfig by role name.
 * The string {@link #value()} matches the keys in draw.ai.models.* (yml).
 * @author 29287
 */
public enum RoleNameTypes {
    FIXER("fixer"),
    LEADER("leader"),
    WORKER("worker");

    private final String value;

    RoleNameTypes(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** Case-insensitive reverse lookup; throws if no match. */
    public static RoleNameTypes from(String value) {
        return Arrays.stream(values())
                .filter(r -> r.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role name: " + value));
    }
}
