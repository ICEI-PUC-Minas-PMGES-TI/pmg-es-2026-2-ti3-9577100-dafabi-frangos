package com.dafabi.inventory.dto;

import java.math.BigDecimal;
import com.dafabi.inventory.domain.LotStatus;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryLotResponse(
        UUID id,
        UUID productId,
        String productName,
        String batch,
        LocalDate receivedAt,
        LocalDate expiry,
        BigDecimal quantity,
        LotStatus status
) {
}
