package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.order.common.OrderStatus;
import com.example.order.common.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;

    private String orderCode;

    private String trackingNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private List<OrderItemResponse> items;
    private BigDecimal subtotalPrice;
    private UUID discountId;
    private BigDecimal discountValue;
    private BigDecimal shippingFee;
    private BigDecimal totalPrice;

    private String recipientAddress;

    private RecipientAddressResponse recipient;

    private String paymentMethodName;
    private String paymentLast4;

    private Instant estimatedDeliveryDate;
    private String carrierName;

    private String carrierDescription;

    private String currentLocation;

    private List<OrderTrackingEventResponse> timeline;

    private Instant createdAt;
}
