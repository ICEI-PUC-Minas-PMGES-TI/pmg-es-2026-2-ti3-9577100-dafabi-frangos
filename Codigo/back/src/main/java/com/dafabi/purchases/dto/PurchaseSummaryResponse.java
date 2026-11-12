package com.dafabi.purchases.dto;

import com.dafabi.purchases.domain.PurchaseStatus;
import com.dafabi.purchases.domain.PurchasePaymentStatus;
import com.dafabi.expenses.domain.ExpensePaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseSummaryResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        LocalDate date,
        Integer itemCount,
        BigDecimal total,
        PurchaseStatus status,
        PurchasePaymentStatus paymentStatus,
        ExpensePaymentMethod paymentMethod,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {
}
