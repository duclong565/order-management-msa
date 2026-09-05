package com.example.product.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductVariantRowProjection {

    UUID getVariantId();

    UUID getProductId();

    String getProductName();

    String getVariantName();

    String getSku();

    String getImageUrl();

    UUID getCategoryId();

    String getCategoryName();

    BigDecimal getPrice();

    long getTotalQuantity();
}
