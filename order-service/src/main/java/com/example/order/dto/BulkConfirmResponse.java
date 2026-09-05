package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkConfirmResponse {
    private int requestedCount;
    private int confirmedCount;
    private List<UUID> confirmedOrderIds;
    private List<SkippedOrder> skipped;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedOrder {
        private UUID orderId;
        private String reason;
    }
}
