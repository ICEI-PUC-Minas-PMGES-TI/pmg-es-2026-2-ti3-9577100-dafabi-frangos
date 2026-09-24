package com.dafabi.cash.dto;

import com.dafabi.cash.domain.CashRegisterStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashRegisterSummaryResponse(
        UUID id,
        UUID storeId,
        String operator,
        CashRegisterStatus status,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        BigDecimal initialBalance,
        BigDecimal cashSales,
        BigDecimal cashExpenses,
        BigDecimal cashRefunds,
        BigDecimal expectedBalance,
        BigDecimal countedBalance,
        BigDecimal difference,
        String justification
) {
}
