package com.example.product.controller;

import com.example.product.common.BaseResponse;
import com.example.product.dto.DecreaseStockRequest;
import com.example.product.dto.ProductVariantResponse;
import com.example.product.service.ProductVariantQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/product-variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantQueryService productVariantQueryService;

    @PostMapping("/get-by-ids")
    public ResponseEntity<BaseResponse<List<ProductVariantResponse>>> getByIds(@RequestBody List<UUID> variantIds) {
        return ResponseEntity.ok(BaseResponse.success(productVariantQueryService.getByIds(variantIds)));
    }

    @GetMapping("/{variantId}/stock")
    public ResponseEntity<BaseResponse<Long>> getStock(@PathVariable UUID variantId) {
        return ResponseEntity.ok(BaseResponse.success(productVariantQueryService.getStock(variantId)));
    }

    @PostMapping("/decrease-stock")
    public ResponseEntity<BaseResponse<Void>> decreaseStock(@Valid @RequestBody DecreaseStockRequest request) {
        productVariantQueryService.decreaseStock(request.getItems());
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
