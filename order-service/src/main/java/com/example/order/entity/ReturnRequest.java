package com.example.order.entity;

import com.example.order.common.ReturnOriginType;
import com.example.order.common.ReturnReason;
import com.example.order.common.ReturnStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
public class ReturnRequest extends BaseEntity {
    @Column(name = "return_code", nullable = false, unique = true, length = 20)
    private String returnCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReturnReason reason;

    @Column(name = "reason_note", length = 500)
    private String reasonNote;

    @Column(name = "origin_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReturnOriginType originType;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReturnStatus status = ReturnStatus.PENDING_ACTION;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    @Column(name = "tracking_number", unique = true, length = 50)
    private String trackingNumber;

    // warehouse thuộc product-service - chỉ lưu ID, lấy chi tiết qua ProductClient
    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "restocked_at")
    private Instant restockedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;
}
