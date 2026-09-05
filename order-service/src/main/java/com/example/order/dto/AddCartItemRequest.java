package com.example.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AddCartItemRequest {

    @NotNull
    private UUID productVariantId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
