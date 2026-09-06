package com.example.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class DecreaseStockLine {

    @NotNull
    private UUID productVariantId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
