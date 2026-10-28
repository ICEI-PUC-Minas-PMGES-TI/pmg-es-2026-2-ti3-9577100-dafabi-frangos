package com.dafabi.expenses.dto;

import com.dafabi.expenses.domain.ExpensePaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseRequest(
        @NotBlank(message = "A descrição da despesa é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,
        @NotBlank(message = "A categoria da despesa é obrigatória.")
        @Size(max = 100, message = "A categoria deve ter no máximo 100 caracteres.")
        String category,
        @NotNull(message = "O valor da despesa é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,
        @NotNull(message = "A data da despesa é obrigatória.")
        @PastOrPresent(message = "A data da despesa não pode ser futura.")
        LocalDate date,
        @NotNull(message = "A forma de pagamento é obrigatória.")
        ExpensePaymentMethod paymentMethod,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observation
) {}
