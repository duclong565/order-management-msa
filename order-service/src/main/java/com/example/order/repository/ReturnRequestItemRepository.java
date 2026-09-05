package com.example.order.repository;

import com.example.order.entity.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, UUID> {
    @Query("""
        select rri
        from ReturnRequestItem rri
        join fetch rri.orderItem oi
        where rri.returnRequest.id = :returnRequestId
          and rri.deleted = false
        """)
    List<ReturnRequestItem> findByReturnRequestId(@Param("returnRequestId") UUID returnRequestId);

    @Query("""
        select coalesce(sum(rri.quantity), 0)
        from ReturnRequestItem rri
        where rri.orderItem.id = :orderItemId
          and rri.deleted = false
        """)
    int totalReturnedQuantity(@Param("orderItemId") UUID orderItemId);
}
