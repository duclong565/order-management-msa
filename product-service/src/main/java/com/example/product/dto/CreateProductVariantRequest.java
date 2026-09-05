package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductVariantRequest {

    @NotBlank
    private String name;

    private String sku;

    @NotNull
    @Positive
    private BigDecimal price;
}
