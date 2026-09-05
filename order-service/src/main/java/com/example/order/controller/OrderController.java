package com.example.order.controller;

import com.example.order.common.BaseResponse;
import com.example.order.dto.OrderListItemResponse;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.PlaceOrderRequest;
import com.example.order.dto.UpdateOrderStatusRequest;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<BaseResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(orderService.placeOrder(request)));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<OrderListItemResponse>>> getMyOrders() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(orderService.getMyOrders()));
    }

    // todo: them user guard
    @GetMapping("/{orderId}")
    public ResponseEntity<BaseResponse<OrderResponse>> getOrderDetail(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(orderService.getOrderDetail(orderId)));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_MANAGER')")
    public ResponseEntity<BaseResponse<OrderResponse>> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(orderService.updateStatus(orderId, request),
                        "Order status updated"));
    }
}
