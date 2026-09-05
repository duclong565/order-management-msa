package com.example.product.common;

import lombok.Getter;

@Getter
public class BaseResponse<T> {
    private static final int SUCCESS_CODE = 0;

    private final int code;
    private final T data;
    private final String message;
    private final Object metadata;

    private BaseResponse(int code, T data, String message, Object metadata) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.metadata = metadata;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(SUCCESS_CODE, data, "Success", null);
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(SUCCESS_CODE, data, message, null);
    }

    public static <T> BaseResponse<T> success(T data, Object metadata) {
        return new BaseResponse<>(SUCCESS_CODE, data, "Success", metadata);
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message, null);
    }
}
