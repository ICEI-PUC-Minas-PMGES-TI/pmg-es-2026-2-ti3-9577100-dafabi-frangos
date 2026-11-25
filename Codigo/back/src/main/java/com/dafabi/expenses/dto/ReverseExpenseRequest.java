package com.dafabi.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReverseExpenseRequest(
        @NotBlank(message = "Informe o motivo do estorno.")
        @Size(min = 5, max = 500, message = "O motivo deve ter entre 5 e 500 caracteres.")
        String reason
) {
}
