package com.example.product.service;

import com.example.product.dto.DiscountResponse;
import com.example.product.entity.Discount;
import com.example.product.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {
    private final DiscountRepository discountRepository;

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
