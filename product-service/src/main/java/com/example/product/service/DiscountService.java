package com.example.product.service;

import com.example.product.common.ErrorCode;
import com.example.product.dto.DiscountResponse;
import com.example.product.entity.Discount;
import com.example.product.exception.ApplicationException;
import com.example.product.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountService {
    private final DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public DiscountResponse getById(UUID id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.DISCOUNT_NOT_FOUND));
        return toDiscountResponse(discount);
    }

    private DiscountResponse toDiscountResponse(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getName(),
                discount.getDescription(),
                discount.getType(),
                discount.getValue(),
                discount.getStartDate(),
                discount.getEndDate()
        );
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getActiveDiscounts() {
        return discountRepository.findActive(Instant.now())
                .stream()
                .map(this::toDiscountResponse)
                .toList();
    }
}
