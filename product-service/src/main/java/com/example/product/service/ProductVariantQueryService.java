package com.example.product.service;

import com.example.product.common.ErrorCode;
import com.example.product.dto.DecreaseStockLine;
import com.example.product.dto.ProductVariantResponse;
import com.example.product.entity.Inventory;
import com.example.product.exception.ApplicationException;
import com.example.product.repository.InventoryRepository;
import com.example.product.repository.ProductVariantRepository;
import com.example.product.repository.ProductVariantRowProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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

    // Cả danh sách nằm trong 1 transaction: variant nào thiếu hàng thì toàn bộ rollback,
    // không để lại trạng thái trừ được vài dòng rồi dừng.
    @Transactional
    public void decreaseStock(List<DecreaseStockLine> lines) {
        // Khoá dòng theo thứ tự variantId cố định để 2 đơn có chung variant không khoá
        // chéo nhau (A chờ B, B chờ A) rồi deadlock.
        List<DecreaseStockLine> ordered = lines.stream()
                .sorted(Comparator.comparing(DecreaseStockLine::getProductVariantId))
                .toList();

        for (DecreaseStockLine line : ordered) {
            decreaseOne(line.getProductVariantId(), line.getQuantity());
        }
    }

    // Trừ dần qua từng kho: variant có thể nằm ở nhiều kho, tổng đủ hàng nhưng
    // không kho nào một mình đủ.
    private void decreaseOne(UUID variantId, int quantity) {
        List<Inventory> rows = inventoryRepository.findAllForUpdateByVariantId(variantId);

        int remaining = quantity;
        for (Inventory row : rows) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(row.getQuantity(), remaining);
            if (take <= 0) {
                continue;
            }
            row.setQuantity(row.getQuantity() - take);
            remaining -= take;
        }

        if (remaining > 0) {
            throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock for variant " + variantId
                            + ": need " + quantity + ", short by " + remaining);
        }

        inventoryRepository.saveAll(rows);
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
