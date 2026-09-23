package com.dafabi.inventory.dto;

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
        Integer quantity,
        LotStatus status
) {
}
