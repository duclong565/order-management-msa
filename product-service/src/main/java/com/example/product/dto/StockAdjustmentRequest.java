package com.example.product.dto;

import com.example.product.common.InventoryReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull
    private UUID warehouseId;

    @NotNull
    private Integer quantityDelta;

    @NotNull
    private InventoryReason reason;

    @Size(max = 500)
    private String note;
}
