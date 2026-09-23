package com.dafabi.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "Informe o produto.") UUID productId,
        @NotNull(message = "Informe a nova quantidade.")
        @Min(value = 0, message = "A quantidade não pode ser negativa.") Integer newQuantity,
        @NotBlank(message = "Informe o motivo do ajuste.")
        @Size(min = 5, max = 500, message = "O motivo deve ter entre 5 e 500 caracteres.") String reason,
        @Size(max = 100, message = "O nome do usuário deve ter no máximo 100 caracteres.") String performedBy
) {
}
