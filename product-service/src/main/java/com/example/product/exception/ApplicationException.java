package com.example.product.exception;

import com.example.product.common.ErrorCode;
import lombok.Getter;

@Getter
public class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
