package com.dafabi.cash.dto;

import com.dafabi.cash.domain.CashMovementDirection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashMovementResponse(
        UUID id,
        UUID cashRegisterId,
        CashMovementDirection direction,
        String type,
        String movementType,
        BigDecimal amount,
        String origin,
        String description,
        String referenceType,
        UUID referenceId,
        OffsetDateTime occurredAt,
        OffsetDateTime date
) {
}
