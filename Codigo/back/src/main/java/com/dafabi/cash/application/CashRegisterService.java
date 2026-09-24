package com.dafabi.cash.application;

import com.dafabi.cash.domain.CashMovement;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.dto.CloseCashRequest;
import com.dafabi.cash.dto.CreateCashMovementRequest;
import com.dafabi.cash.dto.CashMovementResponse;
import com.dafabi.cash.dto.CashRegisterResponse;
import com.dafabi.cash.dto.CashRegisterSummaryResponse;
import com.dafabi.cash.dto.OpenCashRequest;
import com.dafabi.cash.mapper.CashRegisterMapper;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashRegisterMapper cashRegisterMapper;

    public CashRegisterService(CashRegisterRepository cashRegisterRepository,
                               CashMovementRepository cashMovementRepository,
                               CashRegisterMapper cashRegisterMapper) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.cashRegisterMapper = cashRegisterMapper;
    }

    @Transactional
    public CashRegisterResponse openCashRegister(OpenCashRequest request) {
        UUID storeId = request.storeId() != null ? request.storeId() : CashRegister.DEFAULT_STORE_ID;

        if (cashRegisterRepository.existsByStoreIdAndStatus(storeId, CashRegisterStatus.OPEN)) {
            throw new BusinessException("CASH_ALREADY_OPEN", "Já existe um caixa aberto para este estabelecimento.", HttpStatus.CONFLICT);
        }

        BigDecimal initialBalance = request.initialBalance().setScale(2, RoundingMode.HALF_UP);
        CashRegister register = new CashRegister(
                null,
                storeId,
                request.operatorId(),
                request.operator(),
                initialBalance,
                OffsetDateTime.now()
        );

        CashRegister saved = cashRegisterRepository.save(register);
        return cashRegisterMapper.toResponse(saved, initialBalance);
    }

    @Transactional
    public CashRegisterResponse closeCashRegister(UUID id, CloseCashRequest request) {
        CashRegister register = cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", id));

        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BusinessException("CASH_ALREADY_CLOSED", "O caixa já se encontra fechado.", HttpStatus.CONFLICT);
        }

        BigDecimal countedBalance = request.countedBalance().setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedBalance = calculateExpectedBalance(register);
        BigDecimal difference = countedBalance.subtract(expectedBalance).setScale(2, RoundingMode.HALF_UP);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            if (request.justification() == null || request.justification().trim().length() < 4) {
                throw new BusinessException(
                        "JUSTIFICATION_REQUIRED",
                        "A justificativa é obrigatória e deve ter pelo menos 4 caracteres quando há diferença no saldo.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        register.setClosedAt(OffsetDateTime.now());
        register.setExpectedBalanceAtClose(expectedBalance);
        register.setCountedBalance(countedBalance);
        register.setDifference(difference);
        register.setJustification(request.justification() != null ? request.justification().trim() : null);
        register.setStatus(CashRegisterStatus.CLOSED);

        CashRegister saved = cashRegisterRepository.save(register);
        return cashRegisterMapper.toResponse(saved, expectedBalance);
    }

    @Transactional
    public CashRegisterResponse closeCurrentCashRegister(UUID storeId, CloseCashRequest request) {
        UUID resolvedStoreId = storeId != null ? storeId : CashRegister.DEFAULT_STORE_ID;
        CashRegister register = cashRegisterRepository.findByStoreIdAndStatus(resolvedStoreId, CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessException("CASH_CLOSED", "Não existe caixa aberto para encerramento neste estabelecimento.", HttpStatus.NOT_FOUND));

        return closeCashRegister(register.getId(), request);
    }

    @Transactional(readOnly = true)
    public Optional<CashRegisterResponse> findCurrent(UUID storeId) {
        UUID resolvedStoreId = storeId != null ? storeId : CashRegister.DEFAULT_STORE_ID;

        Optional<CashRegister> openRegister = cashRegisterRepository.findByStoreIdAndStatus(resolvedStoreId, CashRegisterStatus.OPEN);
        if (openRegister.isPresent()) {
            CashRegister register = openRegister.get();
            return Optional.of(cashRegisterMapper.toResponse(register, calculateExpectedBalance(register)));
        }

        return cashRegisterRepository.findFirstByStoreIdOrderByOpenedAtDesc(resolvedStoreId)
                .map(register -> cashRegisterMapper.toResponse(register, calculateExpectedBalance(register)));
    }

    @Transactional(readOnly = true)
    public CashRegisterResponse findById(UUID id) {
        CashRegister register = cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", id));
        return cashRegisterMapper.toResponse(register, calculateExpectedBalance(register));
    }

    @Transactional(readOnly = true)
    public CashRegisterSummaryResponse getSummary(UUID id) {
        CashRegister register = cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", id));

        List<CashMovement> movements = cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(id);

        BigDecimal cashSales = movements.stream()
                .filter(m -> m.getDirection() == CashMovementDirection.IN && isSaleMovement(m.getMovementType()))
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashExpenses = movements.stream()
                .filter(m -> m.getDirection() == CashMovementDirection.OUT && isExpenseMovement(m.getMovementType()))
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashRefunds = movements.stream()
                .filter(m -> m.getDirection() == CashMovementDirection.OUT && isRefundMovement(m.getMovementType()))
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedBalance = calculateExpectedBalance(register);

        return cashRegisterMapper.toSummaryResponse(register, cashSales, cashExpenses, cashRefunds, expectedBalance);
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> getMovements(UUID id) {
        if (!cashRegisterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Caixa", id);
        }
        return cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(id).stream()
                .map(cashRegisterMapper::toMovementResponse)
                .toList();
    }

    @Transactional
    public CashMovementResponse createMovement(UUID id, CreateCashMovementRequest request) {
        CashRegister register = cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", id));

        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BusinessException("CASH_CLOSED", "Não é possível registrar movimentação em um caixa fechado.", HttpStatus.CONFLICT);
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        CashMovement movement = new CashMovement(
                register,
                request.direction(),
                request.movementType().trim().toUpperCase(),
                amount,
                request.origin() != null ? request.origin().trim() : null,
                request.description().trim(),
                request.referenceType(),
                request.referenceId()
        );

        CashMovement saved = cashMovementRepository.save(movement);
        return cashRegisterMapper.toMovementResponse(saved);
    }

    @Transactional
    public CashMovement recordMovement(UUID cashRegisterId,
                                       CashMovementDirection direction,
                                       String movementType,
                                       BigDecimal amount,
                                       String origin,
                                       String description,
                                       String referenceType,
                                       UUID referenceId) {
        CashRegister register = cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", cashRegisterId));

        if (register.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BusinessException("CASH_CLOSED", "Não é possível registrar movimentação em um caixa fechado.", HttpStatus.CONFLICT);
        }

        CashMovement movement = new CashMovement(
                register,
                direction,
                movementType,
                amount.setScale(2, RoundingMode.HALF_UP),
                origin,
                description,
                referenceType,
                referenceId
        );
        return cashMovementRepository.save(movement);
    }

    public BigDecimal calculateExpectedBalance(CashRegister register) {
        if (register.getStatus() == CashRegisterStatus.CLOSED && register.getExpectedBalanceAtClose() != null) {
            return register.getExpectedBalanceAtClose().setScale(2, RoundingMode.HALF_UP);
        }

        List<CashMovement> movements = cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(register.getId());

        BigDecimal inTotal = movements.stream()
                .filter(m -> m.getDirection() == CashMovementDirection.IN)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outTotal = movements.stream()
                .filter(m -> m.getDirection() == CashMovementDirection.OUT)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal base = register.getInitialBalance() != null ? register.getInitialBalance() : BigDecimal.ZERO;
        return base.add(inTotal).subtract(outTotal).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isSaleMovement(String type) {
        if (type == null) return false;
        String upper = type.toUpperCase();
        return upper.contains("SALE") || upper.contains("VENDA");
    }

    private boolean isExpenseMovement(String type) {
        if (type == null) return false;
        String upper = type.toUpperCase();
        return upper.contains("EXPENSE") || upper.contains("DESPESA") || upper.contains("SANGRIA");
    }

    private boolean isRefundMovement(String type) {
        if (type == null) return false;
        String upper = type.toUpperCase();
        return upper.contains("REFUND") || upper.contains("ESTORNO");
    }
}
