package com.example.order.repository;

public interface CarrierShareProjection {
    String getCarrierName();

    Boolean getInNetwork();

    long getOrderCount();
}
