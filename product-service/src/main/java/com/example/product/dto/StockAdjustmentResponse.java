package com.example.product.dto;

import com.example.product.common.InventoryReason;
import com.example.product.common.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentResponse {

    private UUID transactionId;
    private UUID variantId;
    private String sku;
    private UUID warehouseId;
    private String warehouseName;

    private int quantityDelta;
    private int quantityBefore;
    private int quantityAfterInWarehouse;

    private long totalQuantityAllWarehouses;
    private StockStatus stockStatus;

    private InventoryReason reason;
    private String note;
    private Instant occurredAt;
}
