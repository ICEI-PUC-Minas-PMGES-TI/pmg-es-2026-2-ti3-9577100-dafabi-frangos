package com.dafabi.inventory.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
        @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 11, fraction = 3, message = "Use no máximo 3 casas decimais.") BigDecimal quantity,
        @Size(max = 100, message = "O nome do usuário deve ter no máximo 100 caracteres.") String performedBy
) {
}
