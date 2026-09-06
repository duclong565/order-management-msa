package com.example.order.client;

import java.util.List;
import java.util.UUID;

public interface ProductClient {

    List<ProductVariantResponse> getVariantsByIds(List<UUID> variantIds);

    long getStock(UUID variantId);

    // Trừ kho cho cả đơn trong 1 lời gọi: product-service xử lý trong 1 transaction,
    // hoặc trừ hết hoặc không trừ gì. Gọi lặp từng dòng sẽ để lại trừ nửa vời.
    void decreaseStock(List<StockDecreaseLine> lines);

    DiscountResponse getDiscountById(UUID discountId);

    WarehouseResponse getWarehouseById(UUID warehouseId);
}
