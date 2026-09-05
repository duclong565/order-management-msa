package com.example.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWarehouseRequest {

    @NotBlank
    private String name;

    private String description;
}
