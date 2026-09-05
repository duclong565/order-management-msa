package com.example.auth.exception;

import com.example.auth.common.BaseResponse;
import com.example.auth.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<BaseResponse<Void>> handleApplicationException(ApplicationException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode ec = ErrorCode.ACCESS_DENIED;
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), ec.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        String msg = "Invalid value for '" + ex.getName() + "': " + ex.getValue();
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        String msg = "Malformed request body";
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = ife.getPath().isEmpty() ? "?" : ife.getPath().getLast().getPropertyName();
            msg = "Invalid value for '" + field + "': " + ife.getValue();
        }
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(BaseResponse.error(ec.getCode(), ec.getMessage()));  // KHONG lo ex.getMessage()
    }
}
