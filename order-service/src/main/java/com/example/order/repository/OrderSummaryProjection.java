package com.example.order.repository;

import java.math.BigDecimal;

public interface OrderSummaryProjection {

    long getTotalOrders();

    long getPendingCount();

    long getShippingCount();

    long getFailedCount();

    long getWithCarrierCount();

    BigDecimal getRevenueThisMonth();

    BigDecimal getRevenueLastMonth();
}
