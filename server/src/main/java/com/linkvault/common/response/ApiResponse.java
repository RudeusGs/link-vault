package com.linkvault.common.response;

public record ApiResponse<T>(boolean success, String message, T data, Object errors) {
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(false, message, data, null);
    }

    public static <T> ApiResponse<T> validationFailure(String message, Object errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
