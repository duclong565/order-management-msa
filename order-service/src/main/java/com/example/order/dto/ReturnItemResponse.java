package com.example.order.dto;

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
public class ReturnItemResponse {
    private UUID returnItemId;
    private UUID orderItemId;
    private UUID productVariantId;
    private String productName;
    private String variantName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
