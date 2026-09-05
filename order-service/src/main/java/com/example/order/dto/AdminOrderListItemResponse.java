package com.example.order.dto;

import com.example.order.common.OrderStatus;
import com.example.order.common.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderListItemResponse {
    private UUID orderId;

    private String orderCode;

    private UUID customerId;

    private String customerName;

    private Instant createdAt;

    private BigDecimal totalPrice;

    private OrderStatus status;
    private PaymentStatus paymentStatus;

    private String carrierName;
}
