package com.example.order.repository;

import java.math.BigDecimal;

public interface ReturnSummaryProjection {

    long getActiveReturns();

    long getThisMonthCount();

    long getLastMonthCount();

    long getAwaitingInspection();

    BigDecimal getAvgCycleHours();

    BigDecimal getTotalRefunds();

    BigDecimal getAvgProcessingThisWeek();

    BigDecimal getAvgProcessingLastWeek();

    long getUrgentInspectionCount();

    long getCarrierDelayCount();
}
