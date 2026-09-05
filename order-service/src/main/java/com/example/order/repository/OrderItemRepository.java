package com.example.order.repository;

import com.example.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
        select oi from OrderItem oi
        join fetch oi.productVariant v
        join fetch v.product
        where oi.order.id = :orderId
        """)
    List<OrderItem> findByOrderIdWithVariant(@Param("orderId") UUID orderId);
}
