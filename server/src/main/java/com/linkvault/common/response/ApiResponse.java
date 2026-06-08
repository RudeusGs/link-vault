package com.linkvault.common.response;

import com.linkvault.common.exception.ErrorCode;
import java.time.Instant;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String errorCode,
    Object details,
    Instant timestamp
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, ErrorCode errorCode) {
        return failure(message, errorCode, null);
    }

    public static <T> ApiResponse<T> failure(String message, ErrorCode errorCode, Object details) {
        return new ApiResponse<>(false, message, null, errorCode.name(), details, Instant.now());
    }

    public static <T> ApiResponse<T> validationFailure(String message, Object details) {
        return failure(message, ErrorCode.VALIDATION_ERROR, details);
    }
}
