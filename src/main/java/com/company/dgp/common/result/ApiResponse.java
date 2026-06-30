package com.company.dgp.common.result;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String requestId
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, null);
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(0, "success", data, requestId);
    }

    public static <T> ApiResponse<T> failure(int code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId);
    }
}
