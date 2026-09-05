package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.order.common.StockStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID cartItemId;
    private UUID productVariantId;
    private String productName;
    private String variantName;
    private BigDecimal unitPrice;
    private int quantity;
    private StockStatus stockStatus;
    private Integer availableQuantity;
}
