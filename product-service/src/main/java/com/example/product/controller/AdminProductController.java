package com.example.product.controller;

import com.example.product.common.BaseResponse;
import com.example.product.common.StockStatus;
import com.example.product.dto.AdminProductListItemResponse;
import com.example.product.dto.CategoryResponse;
import com.example.product.dto.CreateCategoryRequest;
import com.example.product.dto.CreateProductRequest;
import com.example.product.dto.CreateProductVariantRequest;
import com.example.product.dto.ProductResponse;
import com.example.product.dto.ProductVariantSummaryResponse;
import com.example.product.dto.StockAdjustmentRequest;
import com.example.product.dto.StockAdjustmentResponse;
import com.example.product.service.AdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<AdminProductListItemResponse>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminProductListItemResponse> page =
                adminProductService.getProducts(categoryId, stockStatus, search, pageable);

        Map<String, Object> metadata = Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );

        return ResponseEntity.ok(BaseResponse.success(page.getContent(), metadata));
    }

    @GetMapping("/categories")
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(BaseResponse.success(adminProductService.getCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(adminProductService.createCategory(request)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(adminProductService.createProduct(request)));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<BaseResponse<ProductVariantSummaryResponse>> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(adminProductService.createVariant(productId, request)));
    }

    @PostMapping("/{variantId}/stock-adjustments")
    public ResponseEntity<BaseResponse<StockAdjustmentResponse>> adjustStock(
            @PathVariable UUID variantId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        StockAdjustmentResponse result = adminProductService.adjustStock(variantId, request);
        return ResponseEntity.ok(BaseResponse.success(result,
                "Stock adjusted: " + result.getQuantityBefore() + " -> " + result.getQuantityAfterInWarehouse()));
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<StreamingResponseBody> exportExcel(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) String search
    ) {
        StreamingResponseBody body =
                outputStream -> adminProductService.writeExcel(categoryId, stockStatus, search, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
