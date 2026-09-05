package com.example.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private UUID variantId;
    private UUID productId;
    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
    private long totalQuantity;
}
