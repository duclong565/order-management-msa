package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnSummaryResponse {
    private long activeReturns;

    private BigDecimal activeReturnsChangePercent;

    private long awaitingInspection;

    private BigDecimal avgCycleHours;

    private BigDecimal totalRefunds;

    private BigDecimal avgProcessingHours;

    private BigDecimal processingFasterPercent;

    private long urgentInspectionCount;

    private long carrierDelayCount;
}
