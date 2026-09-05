package com.example.order.dto;

import com.example.order.common.ReturnOriginType;
import com.example.order.common.ReturnReason;
import com.example.order.common.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetailResponse {
    private UUID returnId;
    private String returnCode;

    private UUID customerId;
    private String customerName;
    private String customerEmail;

    private UUID orderId;
    private String orderCode;

    private ReturnReason reason;
    private String reasonNote;
    private ReturnOriginType originType;
    private ReturnStatus status;

    private List<ReturnItemResponse> items;
    private BigDecimal refundAmount;

    private String carrierName;
    private String trackingNumber;
    private String warehouseName;

    private Instant createdAt;
    private Instant receivedAt;
    private Instant restockedAt;
    private Instant refundedAt;
}
