package com.dafabi.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenCashRequest(
        @NotNull(message = "O saldo inicial é obrigatório.")
        @DecimalMin(value = "0.00", message = "O saldo inicial não pode ser negativo.")
        BigDecimal initialBalance,

        UUID storeId,
        UUID operatorId,
        String operator
) {
    public OpenCashRequest(BigDecimal initialBalance) {
        this(initialBalance, null, null, null);
    }

    public OpenCashRequest(BigDecimal initialBalance, String operator) {
        this(initialBalance, null, null, operator);
    }
}
