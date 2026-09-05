package com.example.order.repository;

import com.example.order.entity.TrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackingLogRepository extends JpaRepository<TrackingLog, UUID> {
    List<TrackingLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
