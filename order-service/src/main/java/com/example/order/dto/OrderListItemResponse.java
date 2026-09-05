package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.order.common.OrderStatus;
import com.example.order.common.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemResponse {

    private UUID orderId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalPrice;
    private Instant createdAt;
}
