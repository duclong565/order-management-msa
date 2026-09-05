package com.example.order.repository;

import com.example.order.entity.CartItem;
import com.example.order.dto.CartItemRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Query("""
        select ci from CartItem ci
        join fetch ci.productVariant v
        join fetch v.product
        where ci.cart.id = :cartId
        """)
    List<CartItem> findByCartIdWithVariant(@Param("cartId") UUID cartId);

    Optional<CartItem> findByIdAndCartUserId(UUID id, UUID userId);

    @Query("""
        select new com.example.order.dto.CartItemRow(
            ci.id, v.id, p.name, v.name, v.price, ci.quantity,
            coalesce(sum(i.quantity), 0L))
        from CartItem ci
        join ci.productVariant v
        join v.product p
        left join Inventory i on i.productVariant = v
        where ci.cart.id = :cartId
        group by ci.id, v.id, p.name, v.name, v.price, ci.quantity
        """)
    List<CartItemRow> findCartRows(@Param("cartId") UUID cartId);
}
