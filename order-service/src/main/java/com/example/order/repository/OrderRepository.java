package com.example.order.repository;

import com.example.order.common.OrderStatus;
import com.example.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"user", "carrier", "paymentMethod", "recipientAddress", "discount"})
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> findDetailById(@Param("orderId") UUID orderId);

    @EntityGraph(attributePaths = {"user", "carrier", "paymentMethod", "recipientAddress", "discount"})
    @Query("select o from Order o where o.id = :orderId and o.user.id = :userId")
    Optional<Order> findDetailByIdAndUserId(@Param("orderId") UUID orderId, @Param("userId") UUID userId);

    @Query(value = "select nextval('order_code_seq')", nativeQuery = true)
    long nextOrderCodeSequence();

    @EntityGraph(attributePaths = {"user", "carrier"})
    @Query("""
        select o
        from Order o
        where o.deleted = false
          and (:status is null or o.status = :status)
          and (cast(:search as String) is null
               or lower(o.orderCode) like lower(concat('%', cast(:search as String), '%'))
               or lower(coalesce(o.user.fullName, o.user.username))
                   like lower(concat('%', cast(:search as String), '%')))
        """)
    Page<Order> searchForAdmin(@Param("status") OrderStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @Query("select count(o) from Order o where o.deleted = false")
    long countAllActive();

    @Query("select count(o) from Order o where o.deleted = false and o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("""
        select coalesce(sum(o.totalPrice), 0)
        from Order o
        where o.deleted = false
          and o.paymentStatus = com.example.order.common.PaymentStatus.PAID
          and o.status <> com.example.order.common.OrderStatus.CANCELLED
          and o.createdAt >= :from
          and o.createdAt < :to
        """)
    BigDecimal sumPaidRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select count(o)
        from Order o
        where o.deleted = false
          and o.carrier is not null
        """)
    long countWithCarrier();

    @Query("""
        select c.name as carrierName, c.inNetwork as inNetwork, count(o) as orderCount
        from Order o
        join o.carrier c
        where o.deleted = false
        group by c.name, c.inNetwork
        order by count(o) desc
        """)
    List<CarrierShareProjection> carrierDistribution();

    @Query(value = """
        select coalesce(round(
                 100.0 * count(*) filter (
                     where t.first_move is not null
                       and t.first_move - o.created_at <= interval '24 hours')
                 / nullif(count(*), 0), 1), 0)
        from orders o
        left join lateral (
            select min(created_at) as first_move
            from tracking_logs
            where order_id = o.id
              and status <> 'PENDING'
              and deleted = false
        ) t on true
        where o.deleted = false
          and o.created_at >= :from
          and o.created_at < :to
          and (o.status = 'PENDING' or t.first_move is not null)
        """, nativeQuery = true)
    BigDecimal ingestionVelocityPercent(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select o from Order o where o.id in :ids and o.deleted = false")
    List<Order> findAllByIdIn(@Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") UUID orderId);

    @Query(value = """
        select
            count(*)                                                     as "totalOrders",
            count(*) filter (where status = 'PENDING')                   as "pendingCount",
            count(*) filter (where status = 'SHIPPING')                  as "shippingCount",
            count(*) filter (where status = 'FAILED')                    as "failedCount",
            count(*) filter (where carrier_id is not null)               as "withCarrierCount",
            coalesce(sum(total_price) filter (
                where payment_status = 'PAID'
                  and status <> 'CANCELLED'
                  and created_at >= :thisMonthStart
                  and created_at <  :now), 0)                            as "revenueThisMonth",
            coalesce(sum(total_price) filter (
                where payment_status = 'PAID'
                  and status <> 'CANCELLED'
                  and created_at >= :lastMonthStart
                  and created_at <  :thisMonthStart), 0)                 as "revenueLastMonth"
        from orders
        where deleted = false
        """, nativeQuery = true)
    OrderSummaryProjection summarize(@Param("now") Instant now,
                                     @Param("thisMonthStart") Instant thisMonthStart,
                                     @Param("lastMonthStart") Instant lastMonthStart);
}
