package com.dafabi.purchases.dto;

import com.dafabi.purchases.domain.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        LocalDate date,
        Integer itemCount,
        BigDecimal total,
        PurchaseStatus status,
        String createdBy,
        OffsetDateTime createdAt,
        List<PurchaseItemResponse> items
) {
}
