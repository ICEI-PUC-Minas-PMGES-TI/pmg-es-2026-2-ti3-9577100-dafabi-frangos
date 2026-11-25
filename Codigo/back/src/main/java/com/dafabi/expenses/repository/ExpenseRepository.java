package com.dafabi.expenses.repository;

import com.dafabi.expenses.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.UUID;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select expense from Expense expense where expense.id = :id")
    Optional<Expense> findByIdForUpdate(@Param("id") UUID id);
    List<Expense> findByDateGreaterThanEqualAndDateLessThanEqual(LocalDate startDate, LocalDate endDate);
    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Expense> findByPaidAtGreaterThanEqualAndPaidAtLessThan(OffsetDateTime start, OffsetDateTime end);
}
