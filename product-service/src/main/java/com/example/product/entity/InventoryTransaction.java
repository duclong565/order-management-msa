package com.example.product.entity;

import com.example.product.common.InventoryReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
public class InventoryTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private InventoryReason reason;

    @Column(length = 500)
    private String note;
}
