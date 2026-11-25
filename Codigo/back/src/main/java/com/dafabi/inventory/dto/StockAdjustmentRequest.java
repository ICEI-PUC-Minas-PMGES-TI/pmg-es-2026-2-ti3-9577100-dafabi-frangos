package com.dafabi.inventory.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "Informe o produto.") UUID productId,
        @NotNull(message = "Informe a nova quantidade.")
        @DecimalMin(value = "0", message = "A quantidade não pode ser negativa.")
        @Digits(integer = 11, fraction = 3, message = "Use no máximo 3 casas decimais.") BigDecimal newQuantity,
        @NotBlank(message = "Informe o motivo do ajuste.")
        @Size(min = 5, max = 500, message = "O motivo deve ter entre 5 e 500 caracteres.") String reason,
        @Size(max = 100, message = "O nome do usuário deve ter no máximo 100 caracteres.") String performedBy
) {
}
