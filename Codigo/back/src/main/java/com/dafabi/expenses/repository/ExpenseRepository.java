package com.dafabi.expenses.repository;

import com.dafabi.expenses.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
}