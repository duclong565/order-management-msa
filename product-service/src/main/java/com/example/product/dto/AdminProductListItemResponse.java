package com.example.product.dto;

import com.example.product.common.StockStatus;
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
public class AdminProductListItemResponse {

    private UUID variantId;
    private UUID productId;

    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;

    private UUID categoryId;
    private String categoryName;

    private BigDecimal price;
    private long totalQuantity;
    private StockStatus stockStatus;
}
