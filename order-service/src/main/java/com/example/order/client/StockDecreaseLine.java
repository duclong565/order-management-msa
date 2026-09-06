package com.example.order.client;

import java.util.UUID;

public record StockDecreaseLine(UUID productVariantId, int quantity) {
}
