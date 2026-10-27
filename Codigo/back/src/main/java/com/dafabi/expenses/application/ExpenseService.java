package com.dafabi.expenses.application;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.expenses.domain.Expense;
import com.dafabi.expenses.domain.ExpensePaymentMethod;
import com.dafabi.expenses.domain.ExpenseStatus;
import com.dafabi.expenses.dto.CreateExpenseRequest;
import com.dafabi.expenses.dto.ExpenseResponse;
import com.dafabi.expenses.dto.UpdateExpenseRequest;
import com.dafabi.expenses.mapper.ExpenseMapper;
import com.dafabi.expenses.repository.ExpenseRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {
    private static final String EXPENSE_REFERENCE_TYPE = "EXPENSE";

    private final ExpenseRepository expenseRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashRegisterService cashRegisterService;
    private final ExpenseMapper mapper;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CashRegisterRepository cashRegisterRepository,
                          CashMovementRepository cashMovementRepository,
                          CashRegisterService cashRegisterService,
                          ExpenseMapper mapper) {
        this.expenseRepository = expenseRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.cashRegisterService = cashRegisterService;
        this.mapper = mapper;
    }

    @Transactional
    public ExpenseResponse create(CreateExpenseRequest request) {
        Expense expense = new Expense(null, request.description().trim(), request.category().trim(),
                money(request.amount()), request.date(), request.paymentMethod(), normalize(request.observation()),
                ExpenseStatus.PENDING, request.userId(), null, null);
        Expense saved = expenseRepository.save(expense);
        syncCashMovement(saved);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll(LocalDate startDate, LocalDate endDate,
                                         String category, ExpensePaymentMethod paymentMethod,
                                         ExpenseStatus status) {
        Specification<Expense> specification = Specification.where(null);
        if (startDate != null) specification = specification.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("date"), startDate));
        if (endDate != null) specification = specification.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("date"), endDate));
        if (category != null && !category.isBlank()) specification = specification.and((root, q, cb) -> cb.equal(root.get("category"), category.trim()));
        if (paymentMethod != null) specification = specification.and((root, q, cb) -> cb.equal(root.get("paymentMethod"), paymentMethod));
        if (status != null) specification = specification.and((root, q, cb) -> cb.equal(root.get("status"), status));
        return expenseRepository.findAll(specification, Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt")))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID id) {
        return expenseRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa", id));
    }

    @Transactional
    public ExpenseResponse update(UUID id, UpdateExpenseRequest request) {
        Expense expense = findEntity(id);
        ensurePending(expense);
        removeCashMovement(expense.getId());
        expense.setDescription(request.description().trim());
        expense.setCategory(request.category().trim());
        expense.setAmount(money(request.amount()));
        expense.setDate(request.date());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setObservation(normalize(request.observation()));
        Expense saved = expenseRepository.save(expense);
        syncCashMovement(saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public ExpenseResponse cancel(UUID id) {
        Expense expense = findEntity(id);
        ensurePending(expense);
        expense.setStatus(ExpenseStatus.CANCELLED);
        removeCashMovement(expense.getId());
        return mapper.toResponse(expenseRepository.save(expense));
    }

    private Expense findEntity(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa", id));
    }

    private void ensurePending(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BusinessException("EXPENSE_NOT_PENDING",
                    "Somente despesas pendentes podem ser alteradas ou canceladas.", HttpStatus.CONFLICT);
        }
    }

    private void syncCashMovement(Expense expense) {
        if (expense.getPaymentMethod() != ExpensePaymentMethod.CASH || expense.getStatus() == ExpenseStatus.CANCELLED) return;
        CashRegister register = cashRegisterRepository.findByStoreIdAndStatus(
                        CashRegister.DEFAULT_STORE_ID, CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessException("CASH_CLOSED",
                        "Abra o caixa antes de registrar uma despesa em dinheiro.", HttpStatus.CONFLICT));
        cashRegisterService.recordMovement(register.getId(), CashMovementDirection.OUT, "EXPENSE",
                expense.getAmount(), "Despesa em dinheiro", expense.getDescription(),
                EXPENSE_REFERENCE_TYPE, expense.getId());
    }

    private void removeCashMovement(UUID expenseId) {
        List<com.dafabi.cash.domain.CashMovement> movements =
                cashMovementRepository.findByReferenceTypeAndReferenceId(EXPENSE_REFERENCE_TYPE, expenseId);
        if (!movements.isEmpty()) cashMovementRepository.deleteAll(movements);
    }

    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static java.math.BigDecimal money(java.math.BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
