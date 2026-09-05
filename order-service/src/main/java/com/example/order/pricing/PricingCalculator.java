package com.example.order.pricing;

import com.example.order.client.DiscountResponse;
import com.example.order.common.StockStatus;
import com.example.order.common.ErrorCode;
import com.example.order.exception.ApplicationException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Getter
@Component
public class PricingCalculator {

    private final int lowStockThreshold;
    private final BigDecimal shippingFee;

    public PricingCalculator(@Value("${app.low-stock-threshold}") int lowStockThreshold,
                             @Value("${app.shipping-fee}") BigDecimal shippingFee) {
        this.lowStockThreshold = lowStockThreshold;
        this.shippingFee = shippingFee;
    }


    public void validateDiscountActive(DiscountResponse discount) {
        Instant now = Instant.now();

        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new ApplicationException(ErrorCode.DISCOUNT_NOT_ACTIVE);
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new ApplicationException(ErrorCode.DISCOUNT_EXPIRED);
        }
    }

    public StockStatus resolveStockStatus(long stockQuantity) {
        return resolveStockStatus(stockQuantity, 1);
    }

    public StockStatus resolveStockStatus(long stockQuantity, int wantedQuantity) {
        if (stockQuantity <= 0 || wantedQuantity > stockQuantity) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (stockQuantity <= lowStockThreshold) {
            return StockStatus.LIMITED_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    public BigDecimal calculateDiscountAmount(DiscountResponse discount, BigDecimal subtotal) {
        if (discount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = switch (discount.getType()) {
            case PERCENT -> subtotal.multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            case FIXED -> discount.getValue();
        };
        return amount.compareTo(subtotal) > 0 ? subtotal : amount;
    }
}
