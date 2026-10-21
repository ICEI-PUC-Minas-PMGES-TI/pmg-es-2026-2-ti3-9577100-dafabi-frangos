package com.dafabi.suppliers.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSupplierStatusRequest(
        @NotNull(message = "O status ativo é obrigatório.")
        Boolean active
) {
}
