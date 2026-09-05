package com.example.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecreaseStockRequest {

    @NotNull
    @Min(1)
    private Integer quantity;
}
