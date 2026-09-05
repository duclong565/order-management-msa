package com.example.order.dto;

import com.example.order.common.OrderStatus;
import com.example.order.common.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingEventResponse {
    private UUID eventId;

    private String title;

    private String description;

    private OrderStatus status;

    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private String actorName;
    private UserRole actorRole;

    private Instant occurredAt;
}
