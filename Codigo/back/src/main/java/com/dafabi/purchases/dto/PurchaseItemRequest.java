package com.dafabi.purchases.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemRequest(
        @NotNull(message = "O produto é obrigatório.")
        UUID productId,

        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 1, message = "A quantidade deve ser maior que zero.")
        Integer quantity,

        @NotNull(message = "O custo unitário é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = false, message = "O custo unitário deve ser maior que zero.")
        @Digits(integer = 12, fraction = 2, message = "O custo unitário deve ter no máximo 12 dígitos inteiros e 2 decimais.")
        BigDecimal unitCost
) {
}
