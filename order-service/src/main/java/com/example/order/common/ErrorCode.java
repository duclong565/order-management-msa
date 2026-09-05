package com.example.order.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 400 - request sai
    INVALID_REQUEST(4001, HttpStatus.BAD_REQUEST, "Invalid request"),
    INVALID_QUANTITY(4002, HttpStatus.BAD_REQUEST, "Quantity must be at least 1"),

    // 401 / 403
    UNAUTHORIZED(4011, HttpStatus.UNAUTHORIZED, "Authentication required"),
    ACCESS_DENIED(4031, HttpStatus.FORBIDDEN, "Access denied"),

    // 404 - khong tim thay
    USER_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "User not found"),
    CART_NOT_FOUND(4042, HttpStatus.NOT_FOUND, "Cart not found"),
    CART_ITEM_NOT_FOUND(4043, HttpStatus.NOT_FOUND, "Cart item not found"),
    DISCOUNT_NOT_FOUND(4044, HttpStatus.NOT_FOUND, "Discount not found"),
    ADDRESS_NOT_FOUND(4045, HttpStatus.NOT_FOUND, "Address not found"),
    PAYMENT_METHOD_NOT_FOUND(4046, HttpStatus.NOT_FOUND, "Payment method not found"),
    ORDER_NOT_FOUND(4047, HttpStatus.NOT_FOUND, "Order not found"),
    ORDER_ITEM_NOT_FOUND(4048, HttpStatus.NOT_FOUND, "Order item not found"),
    RETURN_NOT_FOUND(4049, HttpStatus.NOT_FOUND, "Return request not found"),
    PRODUCT_VARIANT_NOT_FOUND(40410, HttpStatus.NOT_FOUND, "Product variant not found"),
    WAREHOUSE_NOT_FOUND(40411, HttpStatus.NOT_FOUND, "Warehouse not found"),

    // 409 - vi pham nghiep vu
    USERNAME_EXISTED(4091, HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_EXISTED(4092, HttpStatus.CONFLICT, "Email already exists"),
    CART_EMPTY(4093, HttpStatus.CONFLICT, "Cart is empty"),
    INSUFFICIENT_STOCK(4094, HttpStatus.CONFLICT, "Insufficient stock"),
    DISCOUNT_EXPIRED(4095, HttpStatus.CONFLICT, "Discount has expired"),
    DISCOUNT_NOT_ACTIVE(4096, HttpStatus.CONFLICT, "Discount is not active yet"),
    WRONG_PASSWORD(4097, HttpStatus.CONFLICT, "Old password is incorrect"),
    INVALID_STATUS_TRANSITION(4098, HttpStatus.CONFLICT, "Invalid order status transition"),
    INVALID_RETURN_TRANSITION(4099, HttpStatus.CONFLICT, "Invalid return status transition"),
    RETURN_QUANTITY_EXCEEDED(40910, HttpStatus.CONFLICT, "Return quantity exceeds purchased quantity"),
    ORDER_NOT_RETURNABLE(40911, HttpStatus.CONFLICT, "Order is not eligible for return"),
    STOCK_WOULD_GO_NEGATIVE(40912, HttpStatus.CONFLICT, "Adjustment would make stock negative"),
    ZERO_STOCK_ADJUSTMENT(40913, HttpStatus.CONFLICT, "Quantity delta must not be zero"),

    // 500
    INTERNAL_ERROR(5001, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
