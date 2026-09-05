package com.example.product.controller;

import com.example.product.common.BaseResponse;
import com.example.product.dto.DiscountResponse;
import com.example.product.service.DiscountService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/discounts")
@AllArgsConstructor
public class DiscountController {

    private final DiscountService  discountService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<DiscountResponse>>> getActiveDiscounts() {
        return ResponseEntity.ok(BaseResponse.success(discountService.getActiveDiscounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<DiscountResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(discountService.getById(id)));
    }
}
