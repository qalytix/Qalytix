package com.qalytix.dto.response;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK", OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, OffsetDateTime.now());
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, null, message, OffsetDateTime.now());
    }
}
