package com.example.order.repository;

import com.example.order.common.ReturnStatus;
import com.example.order.entity.ReturnRequest;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    boolean existsByReturnCode(String returnCode);

    @Query(value = "select nextval('return_code_seq')", nativeQuery = true)
    long nextReturnCodeSequence();

    @EntityGraph(attributePaths = {"order"})
    @Query("""
        select r
        from ReturnRequest r
        where r.deleted = false
          and (:status is null or r.status = :status)
          and (cast(:search as String) is null
               or lower(r.returnCode) like lower(concat('%', cast(:search as String), '%')))
        """)
    Page<ReturnRequest> search(@Param("status") ReturnStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"order", "carrier"})
    @Query("""
        select r
        from ReturnRequest r
        where r.id = :id
          and r.deleted = false
        """)
    Optional<ReturnRequest> findDetailById(@Param("id") UUID id);

    @Query("""
        select count(r)
        from ReturnRequest r
        where r.deleted = false
          and r.status in :statuses
        """)
    long countByStatusIn(@Param("statuses") Collection<ReturnStatus> statuses);

    @Query("""
        select count(r)
        from ReturnRequest r
        where r.deleted = false
          and r.createdAt >= :from
          and r.createdAt < :to
        """)
    long countCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select coalesce(sum(r.refundAmount), 0)
        from ReturnRequest r
        where r.deleted = false
          and r.status = com.example.order.common.ReturnStatus.REFUNDED
          and r.refundedAt >= :from
          and r.refundedAt < :to
        """)
    BigDecimal sumRefundedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        select coalesce(avg(extract(epoch from (refunded_at - created_at)) / 3600.0), 0)
        from return_requests
        where deleted = false
          and refunded_at is not null
          and refunded_at >= :from
          and refunded_at < :to
        """, nativeQuery = true)
    double avgProcessingHoursBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        select coalesce(avg(extract(epoch from (received_at - created_at)) / 3600.0), 0)
        from return_requests
        where deleted = false
          and received_at is not null
          and received_at >= :from
          and received_at < :to
        """, nativeQuery = true)
    double avgReceivingHoursBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select count(r)
        from ReturnRequest r
        where r.deleted = false
          and r.status = com.example.order.common.ReturnStatus.WAREHOUSE_RECEIVED
          and r.receivedAt < :threshold
        """)
    long countAwaitingInspectionOlderThan(@Param("threshold") Instant threshold);

    @Query("""
        select count(r)
        from ReturnRequest r
        where r.deleted = false
          and r.status = com.example.order.common.ReturnStatus.IN_TRANSIT
          and r.createdAt < :threshold
        """)
    long countInTransitOlderThan(@Param("threshold") Instant threshold);

    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "200"))
    @Query("""
        select r
        from ReturnRequest r
        join fetch r.order
        where r.deleted = false
          and (:status is null or r.status = :status)
          and (cast(:search as String) is null
               or lower(r.returnCode) like lower(concat('%', cast(:search as String), '%')))
        order by r.createdAt desc
        """)
    Stream<ReturnRequest> streamForExport(@Param("status") ReturnStatus status,
                                          @Param("search") String search);

    @Query(value = """
        select
            count(*) filter (where status not in ('REFUNDED','REJECTED'))            as "activeReturns",
            count(*) filter (where created_at >= :thisMonthStart
                               and created_at <  :now)                               as "thisMonthCount",
            count(*) filter (where created_at >= :lastMonthStart
                               and created_at <  :thisMonthStart)                    as "lastMonthCount",
            count(*) filter (where status = 'WAREHOUSE_RECEIVED')                    as "awaitingInspection",
            coalesce(avg(extract(epoch from (received_at - created_at)) / 3600.0)
                     filter (where received_at >= :monthAgo
                               and received_at <  :now), 0)                          as "avgCycleHours",
            coalesce(sum(refund_amount)
                     filter (where status = 'REFUNDED'
                               and refunded_at >= :quarterStart
                               and refunded_at <  :now), 0)                          as "totalRefunds",
            coalesce(avg(extract(epoch from (refunded_at - created_at)) / 3600.0)
                     filter (where refunded_at >= :weekAgo
                               and refunded_at <  :now), 0)                          as "avgProcessingThisWeek",
            coalesce(avg(extract(epoch from (refunded_at - created_at)) / 3600.0)
                     filter (where refunded_at >= :twoWeeksAgo
                               and refunded_at <  :weekAgo), 0)                      as "avgProcessingLastWeek",
            count(*) filter (where status = 'WAREHOUSE_RECEIVED'
                               and received_at < :inspectionThreshold)               as "urgentInspectionCount",
            count(*) filter (where status = 'IN_TRANSIT'
                               and created_at < :carrierThreshold)                   as "carrierDelayCount"
        from return_requests
        where deleted = false
        """, nativeQuery = true)
    ReturnSummaryProjection summarize(@Param("now") Instant now,
                                      @Param("thisMonthStart") Instant thisMonthStart,
                                      @Param("lastMonthStart") Instant lastMonthStart,
                                      @Param("quarterStart") Instant quarterStart,
                                      @Param("weekAgo") Instant weekAgo,
                                      @Param("twoWeeksAgo") Instant twoWeeksAgo,
                                      @Param("monthAgo") Instant monthAgo,
                                      @Param("inspectionThreshold") Instant inspectionThreshold,
                                      @Param("carrierThreshold") Instant carrierThreshold);
}
