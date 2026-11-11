package com.dafabi.sales.application;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovement;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleItem;
import com.dafabi.sales.domain.SaleRefund;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.CreateSaleRefundRequest;
import com.dafabi.sales.dto.SaleRefundEligibilityResponse;
import com.dafabi.sales.dto.SaleRefundResponse;
import com.dafabi.sales.mapper.SaleRefundMapper;
import com.dafabi.sales.repository.SaleRefundRepository;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.shared.security.RequestUser;
import com.dafabi.shared.security.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class SaleRefundService {

    private static final String SALE_REFERENCE_TYPE = "SALE";
    private static final String REFUND_REFERENCE_TYPE = "SALE_REFUND";
    private static final String REFUND_MOVEMENT_TYPE = "REFUND";
    private static final String REFUND_ORIGIN = "Estorno de venda";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final int MAX_RESPONSIBLE_NAME_LENGTH = 100;
    private static final String LOCAL_USER = "Usuário local";

    private final SaleRepository saleRepository;
    private final SaleRefundRepository saleRefundRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashRegisterService cashRegisterService;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SaleRefundMapper saleRefundMapper;

    public SaleRefundService(SaleRepository saleRepository,
                             SaleRefundRepository saleRefundRepository,
                             CashRegisterRepository cashRegisterRepository,
                             CashMovementRepository cashMovementRepository,
                             CashRegisterService cashRegisterService,
                             ProductStockRepository productStockRepository,
                             StockMovementRepository stockMovementRepository,
                             SaleRefundMapper saleRefundMapper) {
        this.saleRepository = saleRepository;
        this.saleRefundRepository = saleRefundRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.cashRegisterService = cashRegisterService;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.saleRefundMapper = saleRefundMapper;
    }

    @Transactional(readOnly = true)
    public SaleRefundEligibilityResponse findEligibility(UUID saleId) {
        Sale sale = saleRepository.findWithItemsById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        SaleRefund refund = saleRefundRepository.findBySale_Id(saleId).orElse(null);

        Optional<BusinessException> blocker = findBlocker(sale, refund);
        return saleRefundMapper.toEligibilityResponse(
                sale,
                blocker.isPresent() ? BigDecimal.ZERO : cashAmountToReverse(sale),
                blocker.map(BusinessException::getCode).orElse(null),
                blocker.map(BusinessException::getMessage).orElse(null),
                refund);
    }

    @Transactional(readOnly = true)
    public SaleRefundResponse findBySaleId(UUID saleId) {
        if (!saleRepository.existsById(saleId)) {
            throw new ResourceNotFoundException("Venda", saleId);
        }
        return saleRefundRepository.findBySale_Id(saleId)
                .map(saleRefundMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Estorno da venda", saleId));
    }

    @Transactional(readOnly = true)
    public Page<SaleRefundResponse> findAll(Pageable pageable) {
        return saleRefundRepository.findAllByOrderByRefundedAtDesc(pageable)
                .map(saleRefundMapper::toResponse);
    }

    @Transactional
    public SaleRefundOutcome refund(UUID saleId,
                                    CreateSaleRefundRequest request,
                                    RequestUser user,
                                    String idempotencyKey) {
        user.requireRole(UserRole.ADMIN, "Somente administradores podem estornar vendas.");
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        Sale sale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));

        if (normalizedKey != null) {
            Optional<SaleRefund> previous = saleRefundRepository.findByIdempotencyKey(normalizedKey);
            if (previous.isPresent()) {
                if (!previous.get().getSale().getId().equals(saleId)) {
                    throw new BusinessException(
                            "IDEMPOTENCY_KEY_REUSED",
                            "A chave de idempotência informada já foi utilizada em outro estorno.",
                            HttpStatus.CONFLICT);
                }
                return new SaleRefundOutcome(saleRefundMapper.toResponse(previous.get()), false);
            }
        }

        SaleRefund existingRefund = saleRefundRepository.findBySale_Id(saleId).orElse(null);
        Optional<BusinessException> blocker = findBlocker(sale, existingRefund);
        if (blocker.isPresent()) {
            throw blocker.get();
        }

        BigDecimal cashAmount = cashAmountToReverse(sale);
        CashRegister register = null;
        if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
            register = cashRegisterRepository
                    .findByStoreIdAndStatusForUpdate(resolveStoreId(sale), CashRegisterStatus.OPEN)
                    .orElseThrow(() -> new BusinessException(
                            "CASH_CLOSED",
                            "Não existe caixa aberto para devolver o valor em dinheiro. Abra o caixa antes de estornar a venda.",
                            HttpStatus.CONFLICT));
        }

        SaleRefund refund = saleRefundRepository.save(new SaleRefund(
                UUID.randomUUID(),
                sale,
                sale.getTotal().setScale(2, RoundingMode.HALF_UP),
                cashAmount,
                register != null ? register.getId() : null,
                request.reason().trim(),
                user.id(),
                responsibleName(user),
                normalizedKey));

        reverseStock(sale, refund);

        if (register != null) {
            cashRegisterService.recordMovement(
                    register.getId(),
                    CashMovementDirection.OUT,
                    REFUND_MOVEMENT_TYPE,
                    cashAmount,
                    REFUND_ORIGIN,
                    "Estorno da venda nº " + sale.getSaleNumber(),
                    REFUND_REFERENCE_TYPE,
                    refund.getId());
        }

        sale.setStatus(SaleStatus.REFUNDED);
        saleRepository.save(sale);

        return new SaleRefundOutcome(saleRefundMapper.toResponse(refund), true);
    }

    private void reverseStock(Sale sale, SaleRefund refund) {
        Map<UUID, Integer> quantitiesByProduct = new TreeMap<>();
        for (SaleItem item : sale.getItems()) {
            quantitiesByProduct.merge(item.getProduct().getId(), item.getQuantity().intValueExact(), Integer::sum);
        }

        for (Map.Entry<UUID, Integer> entry : quantitiesByProduct.entrySet()) {
            UUID productId = entry.getKey();
            ProductStock stock = productStockRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));

            int previousQuantity = stock.getQuantity();
            int newQuantity = previousQuantity + entry.getValue();
            stock.setQuantity(newQuantity);
            stockMovementRepository.save(new StockMovement(
                    stock.getProduct(), null, StockMovementType.REFUND,
                    previousQuantity, newQuantity, REFUND_ORIGIN,
                    refund.getReason(), refund.getRefundedByName(),
                    REFUND_REFERENCE_TYPE, refund.getId()));
        }
    }

    private Optional<BusinessException> findBlocker(Sale sale, SaleRefund refund) {
        if (refund != null || sale.getStatus() == SaleStatus.REFUNDED) {
            return Optional.of(new BusinessException(
                    "SALE_ALREADY_REFUNDED",
                    String.format("A venda nº %d já foi estornada.", sale.getSaleNumber()),
                    HttpStatus.CONFLICT));
        }
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            return Optional.of(new BusinessException(
                    "SALE_CANCELLED",
                    String.format("A venda nº %d está cancelada e não pode ser estornada.", sale.getSaleNumber()),
                    HttpStatus.CONFLICT));
        }
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            return Optional.of(new BusinessException(
                    "SALE_NOT_REFUNDABLE",
                    String.format("A venda nº %d não está concluída e não pode ser estornada.", sale.getSaleNumber()),
                    HttpStatus.CONFLICT));
        }
        return Optional.empty();
    }

    /**
     * O valor devolvido pelo caixa é exatamente o que entrou nele pela venda, evitando
     * retirar dinheiro de vendas pagas por PIX ou cartão.
     */
    private BigDecimal cashAmountToReverse(Sale sale) {
        return cashMovementRepository.findByReferenceTypeAndReferenceId(SALE_REFERENCE_TYPE, sale.getId()).stream()
                .filter(movement -> movement.getDirection() == CashMovementDirection.IN)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private UUID resolveStoreId(Sale sale) {
        if (sale.getCashRegisterId() == null) {
            return CashRegister.DEFAULT_STORE_ID;
        }
        return cashRegisterRepository.findById(sale.getCashRegisterId())
                .map(CashRegister::getStoreId)
                .orElse(CashRegister.DEFAULT_STORE_ID);
    }

    /**
     * O nome do responsável é gravado no estorno e em {@code stock_movement.performed_by},
     * que aceita no máximo 100 caracteres.
     */
    private String responsibleName(RequestUser user) {
        String name = user.name() == null || user.name().isBlank() ? LOCAL_USER : user.name().trim();
        return name.length() > MAX_RESPONSIBLE_NAME_LENGTH ? name.substring(0, MAX_RESPONSIBLE_NAME_LENGTH) : name;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(
                    "INVALID_IDEMPOTENCY_KEY",
                    "A chave de idempotência deve ter no máximo 100 caracteres.");
        }
        return normalized;
    }
}
