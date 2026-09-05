package com.example.order.client;

import java.util.List;
import java.util.UUID;

public interface ProductClient {

    List<ProductVariantResponse> getVariantsByIds(List<UUID> variantIds);

    long getStock(UUID variantId);

    void decreaseStock(UUID variantId, int quantity);

    DiscountResponse getDiscountById(UUID discountId);

    WarehouseResponse getWarehouseById(UUID warehouseId);
}
