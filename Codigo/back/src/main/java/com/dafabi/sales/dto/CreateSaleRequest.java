package com.dafabi.sales.dto;

import com.dafabi.sales.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(
        @NotEmpty(message = "A venda deve conter pelo menos um item.")
        List<@Valid @NotNull(message = "O item da venda não pode ser nulo.") SaleItemRequest> items,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        PaymentMethod paymentMethod,

        UUID storeId,
        UUID operatorId
) {
    public CreateSaleRequest(List<SaleItemRequest> items, PaymentMethod paymentMethod) {
        this(items, paymentMethod, null, null);
    }
}
