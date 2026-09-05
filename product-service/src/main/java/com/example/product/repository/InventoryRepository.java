package com.example.product.repository;

import com.example.product.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    @Query("""
        select coalesce(sum(i.quantity), 0L)
        from Inventory i
        where i.productVariant.id = :variantId
        """)
    long totalStock(@Param("variantId") UUID variantId);

    @Modifying
    @Query(
            """
        update Inventory i
        set i.quantity = i.quantity - :qty
        where i.productVariant.id = :variantId
          and i.quantity >= :qty
        """
    )
    int decreaseStock(@Param("variantId") UUID variantId, @Param("qty") int qty);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select i
        from Inventory i
        where i.productVariant.id = :variantId
          and i.warehouse.id = :warehouseId
          and i.deleted = false
        """)
    Optional<Inventory> findForUpdate(@Param("variantId") UUID variantId,
                                      @Param("warehouseId") UUID warehouseId);
}
