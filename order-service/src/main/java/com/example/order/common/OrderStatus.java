package com.example.order.common;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PICKING,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    FAILED,
    RETURNING,
    REATTEMPT;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(PENDING,   EnumSet.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(PICKING, CANCELLED));
        TRANSITIONS.put(PICKING,   EnumSet.of(SHIPPING, CANCELLED));
        TRANSITIONS.put(SHIPPING,  EnumSet.of(DELIVERED, FAILED));
        TRANSITIONS.put(FAILED,    EnumSet.of(REATTEMPT, RETURNING));
        TRANSITIONS.put(REATTEMPT, EnumSet.of(SHIPPING));
        TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(RETURNING, EnumSet.noneOf(OrderStatus.class));
    }

    private static final Set<OrderStatus> RETURNABLE = EnumSet.of(DELIVERED, FAILED);

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isReturnable() {
        return RETURNABLE.contains(this);
    }
}
