package com.drawai.trigger.error;

public enum ErrorCode {
    OK(0, "ok"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    INTERNAL(500, "internal error");

    private final int code;
    private final String message;

    ErrorCode(int c, String m) {
        this.code = c;
        this.message = m;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
