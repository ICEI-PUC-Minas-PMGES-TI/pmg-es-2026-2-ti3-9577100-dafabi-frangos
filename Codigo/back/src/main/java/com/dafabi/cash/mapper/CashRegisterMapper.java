package com.dafabi.cash.mapper;

import com.dafabi.cash.domain.CashMovement;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.dto.CashMovementResponse;
import com.dafabi.cash.dto.CashRegisterResponse;
import com.dafabi.cash.dto.CashRegisterSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CashRegisterMapper {

    public CashRegisterResponse toResponse(CashRegister register, BigDecimal expectedBalance) {
        if (register == null) return null;

        BigDecimal resolvedExpectedBalance = expectedBalance;
        if (resolvedExpectedBalance == null) {
            resolvedExpectedBalance = register.getStatus() == CashRegisterStatus.CLOSED && register.getExpectedBalanceAtClose() != null
                    ? register.getExpectedBalanceAtClose()
                    : register.getInitialBalance();
        }

        return new CashRegisterResponse(
                register.getId(),
                register.getStoreId(),
                register.getOperatorId(),
                register.getOperatorName(),
                register.getOpenedAt(),
                register.getClosedAt(),
                normalize(register.getInitialBalance()),
                normalize(resolvedExpectedBalance),
                normalize(register.getExpectedBalanceAtClose()),
                normalize(register.getCountedBalance()),
                normalize(register.getDifference()),
                register.getJustification(),
                register.getStatus()
        );
    }

    public CashRegisterSummaryResponse toSummaryResponse(
            CashRegister register,
            BigDecimal cashSales,
            BigDecimal cashExpenses,
            BigDecimal cashRefunds,
            BigDecimal expectedBalance) {
        if (register == null) return null;

        return new CashRegisterSummaryResponse(
                register.getId(),
                register.getStoreId(),
                register.getOperatorName(),
                register.getStatus(),
                register.getOpenedAt(),
                register.getClosedAt(),
                normalize(register.getInitialBalance()),
                normalize(cashSales),
                normalize(cashExpenses),
                normalize(cashRefunds),
                normalize(expectedBalance),
                normalize(register.getCountedBalance()),
                normalize(register.getDifference()),
                register.getJustification()
        );
    }

    public CashMovementResponse toMovementResponse(CashMovement movement) {
        if (movement == null) return null;

        return new CashMovementResponse(
                movement.getId(),
                movement.getCashRegister().getId(),
                movement.getDirection(),
                movement.getDirection().name(),
                movement.getMovementType(),
                normalize(movement.getAmount()),
                movement.getOrigin() != null ? movement.getOrigin() : movement.getMovementType(),
                movement.getDescription(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getOccurredAt(),
                movement.getOccurredAt()
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
