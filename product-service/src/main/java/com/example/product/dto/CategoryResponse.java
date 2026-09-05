package com.example.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
}
