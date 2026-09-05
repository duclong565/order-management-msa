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
public class ProductVariantSummaryResponse {

    private UUID variantId;
    private UUID productId;
    private String name;
    private String sku;
    private BigDecimal price;
}
