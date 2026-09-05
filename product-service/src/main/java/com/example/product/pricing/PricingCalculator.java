package com.example.product.pricing;

import com.example.product.common.StockStatus;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PricingCalculator {

    private final int lowStockThreshold;

    public PricingCalculator(@Value("${app.low-stock-threshold}") int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public StockStatus resolveStockStatus(long stockQuantity) {
        if (stockQuantity <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (stockQuantity <= lowStockThreshold) {
            return StockStatus.LIMITED_STOCK;
        }
        return StockStatus.IN_STOCK;
    }
}
