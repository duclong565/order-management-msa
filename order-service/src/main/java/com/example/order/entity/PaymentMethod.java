package com.example.order.entity;

import com.example.order.common.PaymentMethodType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
public class PaymentMethod extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;
}
