package com.example.product.repository;

import com.example.product.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {

    @Query("""
        select d from Discount d
        where d.deleted = false
          and (d.startDate is null or d.startDate <= :now)
          and (d.endDate   is null or d.endDate   >= :now)
        order by d.name
        """)
    List<Discount> findActive(@Param("now")Instant now);
}
