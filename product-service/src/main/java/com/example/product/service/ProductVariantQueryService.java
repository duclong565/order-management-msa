package com.example.product.service;

import com.example.product.common.ErrorCode;
import com.example.product.dto.ProductVariantResponse;
import com.example.product.exception.ApplicationException;
import com.example.product.repository.InventoryRepository;
import com.example.product.repository.ProductVariantRepository;
import com.example.product.repository.ProductVariantRowProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantQueryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getByIds(List<UUID> variantIds) {
        return productVariantRepository.findRowsByIds(variantIds)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getStock(UUID variantId) {
        return inventoryRepository.totalStock(variantId);
    }

    @Transactional
    public void decreaseStock(UUID variantId, int quantity) {
        int updated = inventoryRepository.decreaseStock(variantId, quantity);
        if (updated == 0) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock for variant: " + variantId);
        }
    }

    private ProductVariantResponse toResponse(ProductVariantRowProjection row) {
        return new ProductVariantResponse(
                row.getVariantId(),
                row.getProductId(),
                row.getProductName(),
                row.getVariantName(),
                row.getSku(),
                row.getImageUrl(),
                row.getPrice(),
                row.getTotalQuantity()
        );
    }
}
