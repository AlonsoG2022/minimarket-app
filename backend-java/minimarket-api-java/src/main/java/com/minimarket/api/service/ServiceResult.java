package com.minimarket.api.service;

public record ServiceResult<T>(boolean success, String error, T data) {

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, null, data);
    }

    public static <T> ServiceResult<T> failure(String error) {
        return new ServiceResult<>(false, error, null);
    }
}
