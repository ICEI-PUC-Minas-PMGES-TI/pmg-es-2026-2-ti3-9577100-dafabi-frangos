package com.dafabi.cash.dto;

import com.dafabi.cash.domain.CashMovementDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCashMovementRequest(
        @NotNull(message = "A direção da movimentação é obrigatória (IN ou OUT).")
        CashMovementDirection direction,

        @NotBlank(message = "O tipo da movimentação é obrigatório.")
        String movementType,

        @NotNull(message = "O valor da movimentação é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @NotBlank(message = "A descrição da movimentação é obrigatória.")
        String description,

        String origin,
        String referenceType,
        UUID referenceId
) {
}
