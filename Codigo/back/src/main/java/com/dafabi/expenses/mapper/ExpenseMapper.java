package com.dafabi.expenses.mapper;

import com.dafabi.expenses.domain.Expense;
import com.dafabi.expenses.dto.ExpenseResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class ExpenseMapper {
    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(), expense.getDescription(), expense.getCategory(),
                expense.getAmount() == null ? null : expense.getAmount().setScale(2, RoundingMode.HALF_UP),
                expense.getDate(), expense.getPaymentMethod(), expense.getObservation(),
                expense.getStatus(), expense.getUserId(), expense.getCreatedAt(), expense.getUpdatedAt());
    }
}
