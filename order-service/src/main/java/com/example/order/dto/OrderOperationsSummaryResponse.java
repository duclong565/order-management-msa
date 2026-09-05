package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderOperationsSummaryResponse {
    private BigDecimal totalRevenueMtd;

    private BigDecimal revenueChangePercent;

    private long totalOrders;

    private long pendingCount;
    private long shippingCount;
    private long failedCount;

    private BigDecimal failedRatePercent;

    private BigDecimal ingestionVelocityPercent;

    private BigDecimal carrierConfirmationRatePercent;

    private BigDecimal inNetworkPercent;
    private List<CarrierShareResponse> carrierDistribution;
}
