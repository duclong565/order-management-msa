package com.example.product.entity;

import com.example.product.common.DiscountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discounts")
@Getter
@Setter
public class Discount extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DiscountType type;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;
}
