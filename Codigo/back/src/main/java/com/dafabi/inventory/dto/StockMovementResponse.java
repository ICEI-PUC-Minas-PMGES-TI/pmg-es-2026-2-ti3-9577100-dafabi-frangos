package com.dafabi.inventory.dto;

import com.dafabi.inventory.domain.StockMovementType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID productId,
        String productName,
        OffsetDateTime date,
        StockMovementType type,
        Integer previousQuantity,
        Integer newQuantity,
        Integer difference,
        String origin,
        String user,
        String reason
) {
}
