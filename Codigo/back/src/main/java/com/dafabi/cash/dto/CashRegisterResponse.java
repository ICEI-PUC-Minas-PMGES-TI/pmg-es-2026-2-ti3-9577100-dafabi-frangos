package com.dafabi.cash.dto;

import com.dafabi.cash.domain.CashRegisterStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CashRegisterResponse(
        UUID id,
        UUID storeId,
        UUID operatorId,
        String operator,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        BigDecimal initialBalance,
        BigDecimal expectedBalance,
        BigDecimal expectedBalanceAtClose,
        BigDecimal countedBalance,
        BigDecimal difference,
        String justification,
        CashRegisterStatus status
) {
}
