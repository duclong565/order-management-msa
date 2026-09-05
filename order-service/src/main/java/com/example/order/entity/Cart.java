package com.example.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
public class Cart extends BaseEntity {

    // user thuộc auth-service - chỉ lưu ID, lấy chi tiết qua UserClient
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // discount thuộc product-service - chỉ lưu ID, lấy chi tiết qua ProductClient
    @Column(name = "discount_id")
    private UUID discountId;
}
