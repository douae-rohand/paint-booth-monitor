package com.projet.config;

import java.time.LocalDateTime;

public class ApiErrorResponse {
    private int code;
    private String message;
    private LocalDateTime timestamp;

    public ApiErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
