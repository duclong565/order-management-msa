package com.example.order.entity;

import com.example.order.common.CarrierType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "carriers")
@Getter
@Setter
public class Carrier extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CarrierType type;

    @Column(name = "in_network", nullable = false)
    private boolean inNetwork = true;

    // address thuộc auth-service - chỉ lưu ID, lấy chi tiết qua UserClient
    @Column(name = "address_id")
    private UUID addressId;
}
