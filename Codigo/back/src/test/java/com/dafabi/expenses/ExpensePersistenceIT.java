package com.dafabi.expenses;

import com.dafabi.expenses.domain.Expense;
import com.dafabi.expenses.domain.ExpensePaymentMethod;
import com.dafabi.expenses.domain.ExpenseStatus;
import com.dafabi.expenses.repository.ExpenseRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ExpensePersistenceIT {

  @Autowired
  private ExpenseRepository expenseRepository;

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    expenseRepository.deleteAll();
  }

  @Test
  void shouldPersistExpenseWithGeneratedIdAndTimestamps() {
    Expense expense = validExpense();

    Expense saved = expenseRepository.saveAndFlush(expense);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.PENDING);
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    Expense persisted = expenseRepository.findById(saved.getId()).orElseThrow();
    assertThat(persisted.getDescription()).isEqualTo("Compra de gás");
    assertThat(persisted.getPaymentMethod()).isEqualTo(ExpensePaymentMethod.BOLETO_TRANSFER);
  }

  @Test
  void shouldRejectMissingRequiredFieldsAndNonPositiveAmount() {
    Expense expense = new Expense();
    expense.setAmount(BigDecimal.ZERO);

    Set<ConstraintViolation<Expense>> violations = validator.validate(expense);

    assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
        .extracting(Object::toString)
        .contains("description", "category", "amount", "date", "paymentMethod");
  }

  private Expense validExpense() {
    return new Expense(
        null,
        "Compra de gás",
        "Operacional",
        new BigDecimal("120.00"),
        LocalDate.of(2026, 10, 21),
        ExpensePaymentMethod.BOLETO_TRANSFER,
        "Botijão da cozinha",
        null,
        UUID.randomUUID(),
        null,
        null);
  }
}