package com.dafabi.inventory.dto;

import java.math.BigDecimal;
import com.dafabi.inventory.domain.StockMovementType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID productId,
        String productName,
        OffsetDateTime date,
        StockMovementType type,
        BigDecimal previousQuantity,
        BigDecimal newQuantity,
        BigDecimal difference,
        String origin,
        String user,
        String reason
) {
}
