package com.example.order.repository;

import com.example.order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByIdAndCartUserId(UUID id, UUID userId);

    Optional<CartItem> findByCartIdAndProductVariantId(UUID cartId, UUID productVariantId);
}
