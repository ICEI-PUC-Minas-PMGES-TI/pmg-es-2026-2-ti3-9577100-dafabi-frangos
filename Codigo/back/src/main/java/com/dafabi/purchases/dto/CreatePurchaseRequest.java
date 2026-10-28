package com.dafabi.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseRequest(
        @NotNull(message = "O fornecedor é obrigatório.")
        UUID supplierId,

        @NotNull(message = "A data da compra é obrigatória.")
        @PastOrPresent(message = "A data da compra não pode ser futura.")
        LocalDate date,

        @NotEmpty(message = "A compra deve conter pelo menos um item.")
        List<@Valid @NotNull(message = "O item da compra não pode ser nulo.") PurchaseItemRequest> items,

        @Size(max = 150, message = "O nome do usuário deve ter no máximo 150 caracteres.")
        String performedBy
) {
    public CreatePurchaseRequest(UUID supplierId, LocalDate date, List<PurchaseItemRequest> items) {
        this(supplierId, date, items, null);
    }
}
