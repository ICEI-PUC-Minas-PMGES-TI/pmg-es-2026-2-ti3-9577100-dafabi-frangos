package com.dafabi.purchases.dto;

import com.dafabi.expenses.domain.ExpensePaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PayPurchaseRequest(@NotNull ExpensePaymentMethod paymentMethod) {
}
