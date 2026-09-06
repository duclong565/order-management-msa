package com.example.product.repository;

import com.example.product.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    @Query("""
        select coalesce(sum(i.quantity), 0L)
        from Inventory i
        where i.productVariant.id = :variantId
          and i.deleted = false
        """)
    long totalStock(@Param("variantId") UUID variantId);

    // Khoá toàn bộ dòng tồn kho của variant để trừ dần theo từng kho.
    // Kho nhiều hàng nhất đứng trước để hạn chế phải tách đơn ra nhiều kho.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select i
        from Inventory i
        where i.productVariant.id = :variantId
          and i.deleted = false
        order by i.quantity desc
        """)
    List<Inventory> findAllForUpdateByVariantId(@Param("variantId") UUID variantId);

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
