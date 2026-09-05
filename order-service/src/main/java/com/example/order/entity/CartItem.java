package com.example.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cart_items_cart_variant",
                columnNames = {"cart_id", "product_variant_id"}
        )
)
@Getter
@Setter
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // productVariant thuộc product-service - chỉ lưu ID, lấy chi tiết qua ProductClient
    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(nullable = false)
    private int quantity;
}
