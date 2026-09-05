package com.example.order.common;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ReturnStatus {
    PENDING_ACTION,
    IN_TRANSIT,
    WAREHOUSE_RECEIVED,
    RESTOCKED,
    REFUNDED,
    REJECTED;

    private static final Map<ReturnStatus, Set<ReturnStatus>> TRANSITIONS =
            new EnumMap<>(ReturnStatus.class);

    static {
        TRANSITIONS.put(PENDING_ACTION,     EnumSet.of(IN_TRANSIT, REJECTED));
        TRANSITIONS.put(IN_TRANSIT,         EnumSet.of(WAREHOUSE_RECEIVED));
        TRANSITIONS.put(WAREHOUSE_RECEIVED, EnumSet.of(RESTOCKED, REFUNDED, REJECTED));
        TRANSITIONS.put(RESTOCKED,          EnumSet.of(REFUNDED));
        TRANSITIONS.put(REFUNDED,           EnumSet.noneOf(ReturnStatus.class));
        TRANSITIONS.put(REJECTED,           EnumSet.noneOf(ReturnStatus.class));
    }

    private static final Set<ReturnStatus> TERMINAL = EnumSet.of(REFUNDED, REJECTED);

    private static final Set<ReturnStatus> AWAITING_INSPECTION = EnumSet.of(WAREHOUSE_RECEIVED);

    public boolean canTransitionTo(ReturnStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean isActive() {
        return !isTerminal();
    }

    public static Set<ReturnStatus> activeStatuses() {
        return EnumSet.complementOf(EnumSet.copyOf(TERMINAL));
    }

    public static Set<ReturnStatus> awaitingInspectionStatuses() {
        return EnumSet.copyOf(AWAITING_INSPECTION);
    }
}
