package com.dafabi.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseCashRequest(
        @NotNull(message = "O valor contado é obrigatório.")
        @DecimalMin(value = "0.00", message = "O valor contado não pode ser negativo.")
        BigDecimal countedBalance,

        String justification
) {
    public CloseCashRequest(BigDecimal countedBalance) {
        this(countedBalance, null);
    }
}
