package com.dafabi.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateInventoryLotRequest(
        @NotNull(message = "Informe o produto.") UUID productId,
        @NotBlank(message = "Informe o lote.")
        @Size(max = 100, message = "O lote deve ter no máximo 100 caracteres.") String batch,
        @NotNull(message = "Informe a data de entrada.") LocalDate receivedAt,
        @NotNull(message = "Informe a validade.") LocalDate expiry,
        @NotNull(message = "Informe a quantidade recebida.")
        @Min(value = 1, message = "A quantidade recebida deve ser maior que zero.") Integer quantity,
        @Size(max = 100, message = "O nome do usuário deve ter no máximo 100 caracteres.") String performedBy
) {
}
