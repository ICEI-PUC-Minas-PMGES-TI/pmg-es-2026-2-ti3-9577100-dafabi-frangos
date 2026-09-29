package com.dafabi.sales.dto;

import com.dafabi.sales.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(
        @NotEmpty(message = "A venda deve conter pelo menos um item.")
        List<@Valid @NotNull(message = "O item da venda não pode ser nulo.") SaleItemRequest> items,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        PaymentMethod paymentMethod,

        @DecimalMin(value = "0.00", message = "O valor recebido não pode ser negativo.")
        BigDecimal amountReceived,

        UUID storeId,
        UUID operatorId
    ) {
    public CreateSaleRequest(List<SaleItemRequest> items, PaymentMethod paymentMethod) {
        this(items, paymentMethod, null, null, null);
    }

    public CreateSaleRequest(List<SaleItemRequest> items, PaymentMethod paymentMethod, BigDecimal amountReceived) {
        this(items, paymentMethod, amountReceived, null, null);
    }

    public CreateSaleRequest(List<SaleItemRequest> items, PaymentMethod paymentMethod, UUID storeId, UUID operatorId) {
        this(items, paymentMethod, null, storeId, operatorId);
    }
}
