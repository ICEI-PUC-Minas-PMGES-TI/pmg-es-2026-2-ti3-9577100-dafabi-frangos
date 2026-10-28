package com.dafabi.expenses.dto;

import com.dafabi.expenses.domain.ExpensePaymentMethod;
import com.dafabi.expenses.domain.ExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        String description,
        String category,
        BigDecimal amount,
        LocalDate date,
        ExpensePaymentMethod paymentMethod,
        String observation,
        ExpenseStatus status,
        UUID userId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
