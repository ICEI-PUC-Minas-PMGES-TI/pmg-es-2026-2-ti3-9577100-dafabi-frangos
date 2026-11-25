package com.dafabi.sales.application;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleItem;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.CreateSaleRequest;
import com.dafabi.sales.dto.SaleItemRequest;
import com.dafabi.sales.dto.SaleResponse;
import com.dafabi.sales.mapper.SaleMapper;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.shared.security.RequestUser;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class SaleService {

    private static final String SALE_REFERENCE_TYPE = "SALE";

    private final SaleRepository saleRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterService cashRegisterService;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final SaleMapper saleMapper;

    public SaleService(SaleRepository saleRepository,
                       CashRegisterRepository cashRegisterRepository,
                       CashRegisterService cashRegisterService,
                       ProductStockRepository productStockRepository,
                       StockMovementRepository stockMovementRepository,
                       InventoryLotRepository inventoryLotRepository,
                       SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashRegisterService = cashRegisterService;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.saleMapper = saleMapper;
    }

    @Transactional
    public SaleResponse create(CreateSaleRequest request) {
        return createInternal(request, null);
    }

    @Transactional
    public SaleResponse create(CreateSaleRequest request, RequestUser user) {
        return createInternal(request, user);
    }

    private SaleResponse createInternal(CreateSaleRequest request, RequestUser user) {
        UUID storeId = request.storeId() != null ? request.storeId() : CashRegister.DEFAULT_STORE_ID;

        CashRegister register = cashRegisterRepository
                .findByStoreIdAndStatusForUpdate(storeId, CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessException(
                        "CASH_CLOSED",
                        "Não existe caixa aberto para o operador. Abra o caixa antes de registrar vendas.",
                        HttpStatus.CONFLICT));

        UUID operatorId = user == null ? resolveOperator(register, request.operatorId()) : user.id();
        String operatorName = user == null ? register.getOperatorName() : user.name();

        Map<UUID, Integer> quantitiesByProduct = new TreeMap<>();
        for (SaleItemRequest item : request.items()) {
            quantitiesByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Sale sale = new Sale(
                UUID.randomUUID(),
                register.getId(),
                operatorId,
                saleRepository.findMaxSaleNumber() + 1,
                OffsetDateTime.now(),
                SaleOrigin.COUNTER,
                request.paymentMethod(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                SaleStatus.COMPLETED);
        sale.setOperatorName(operatorName);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> entry : quantitiesByProduct.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            ProductStock stock = productStockRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
            Product product = stock.getProduct();

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException(
                        "PRODUCT_INACTIVE",
                        String.format("O produto \"%s\" está inativo e não pode ser vendido.", product.getName()));
            }

            int previousQuantity = stock.getQuantity();
            if (quantity > previousQuantity) {
                throw new BusinessException(
                        "INSUFFICIENT_STOCK",
                        String.format("Estoque insuficiente para \"%s\": solicitado %d, disponível %d.",
                                product.getName(), quantity, previousQuantity),
                        HttpStatus.CONFLICT);
            }

            BigDecimal unitPrice = product.getSalePrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            sale.addItem(new SaleItem(null, sale, product, BigDecimal.valueOf(quantity), unitPrice,
                    product.getCurrentCost().setScale(2, RoundingMode.HALF_UP), itemSubtotal));
            subtotal = subtotal.add(itemSubtotal);

            int newQuantity = previousQuantity - quantity;
            consumeLotsForSale(product, quantity, previousQuantity, newQuantity, sale, operatorName);
            stock.setQuantity(newQuantity);
        }

        sale.setSubtotal(subtotal);
        sale.setTotal(subtotal);
        if (request.paymentMethod() == PaymentMethod.CASH
                && request.amountReceived() != null
                && request.amountReceived().compareTo(subtotal) < 0) {
            throw new BusinessException(
                    "INSUFFICIENT_AMOUNT",
                    "O valor recebido é insuficiente para concluir a venda.",
                    HttpStatus.BAD_REQUEST);
        }
        Sale saved = saleRepository.save(sale);

        if (saved.getPaymentMethod() == PaymentMethod.CASH) {
            cashRegisterService.recordMovement(
                    register.getId(),
                    CashMovementDirection.IN,
                    "SALE",
                    saved.getTotal(),
                    "Venda balcão",
                    "Venda nº " + saved.getSaleNumber(),
                    SALE_REFERENCE_TYPE,
                    saved.getId());
        }

        return saleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(UUID id) {
        return saleRepository.findWithItemsById(id)
                .map(saleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", id));
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> findAll(String query,
                                      OffsetDateTime startDate,
                                      OffsetDateTime endDate,
                                      SaleOrigin origin,
                                      SaleStatus status,
                                      PaymentMethod paymentMethod,
                                      UUID operatorId,
                                      Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return saleRepository.findAllWithFilters(
                        normalizedQuery,
                        normalizedQuery != null,
                        startDate,
                        startDate != null,
                        endDate,
                        endDate != null,
                        origin,
                        origin != null,
                        status,
                        status != null,
                        paymentMethod,
                        paymentMethod != null,
                        operatorId,
                        operatorId != null,
                        pageable)
                .map(saleMapper::toResponse);
    }

    private UUID resolveOperator(CashRegister register, UUID requestedOperatorId) {
        UUID registerOperatorId = register.getOperatorId();
        if (requestedOperatorId != null && registerOperatorId != null && !requestedOperatorId.equals(registerOperatorId)) {
            throw new BusinessException(
                    "CASH_CLOSED",
                    "Não existe caixa aberto para o operador informado.",
                    HttpStatus.CONFLICT);
        }

        UUID operatorId = requestedOperatorId != null ? requestedOperatorId : registerOperatorId;
        if (operatorId == null) {
            throw new BusinessException("OPERATOR_REQUIRED", "O operador da venda é obrigatório.");
        }
        return operatorId;
    }

    @Transactional(readOnly = true)
    public BigDecimal completedCounterTotal(String query, OffsetDateTime start, OffsetDateTime end,
            SaleOrigin origin, SaleStatus status, PaymentMethod payment, UUID operator) {
        String q = query == null || query.isBlank() ? null : query.trim();
        return saleRepository.sumCompletedCounterSales(q, q != null, start, start != null, end, end != null,
                origin, origin != null, status, status != null, payment, payment != null, operator, operator != null);
    }

    private void consumeLotsForSale(Product product, int quantity, int aggregateBefore, int aggregateAfter,
                                    Sale sale, String operator) {
        int remaining = quantity;
        int aggregateBalance = aggregateBefore;
        if (product.isPerishable()) {
            LocalDate businessDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
            List<InventoryLot> validLots = inventoryLotRepository.findAvailableByProductForUpdate(product.getId())
                    .stream().filter(lot -> !lot.getExpiry().isBefore(businessDate)).toList();
            int sellable = validLots.stream().mapToInt(InventoryLot::getQuantity).sum();
            if (remaining > sellable) {
                throw new BusinessException("INSUFFICIENT_SELLABLE_STOCK",
                        String.format("Estoque vendável insuficiente para \"%s\": solicitado %d, vendável %d, saldo total %d. O saldo pode incluir lotes vencidos ou sem rastreabilidade.",
                                product.getName(), quantity, sellable, aggregateBefore), HttpStatus.CONFLICT);
            }
            for (InventoryLot lot : validLots) {
                if (remaining == 0) break;
                int consumed = Math.min(remaining, lot.getQuantity());
                int lotBefore = lot.getQuantity();
                lot.setQuantity(lotBefore - consumed);
                remaining -= consumed;
                int nextBalance = aggregateBalance - consumed;
                stockMovementRepository.save(new StockMovement(product, lot, StockMovementType.COUNTER_SALE,
                        aggregateBalance, nextBalance, "Venda balcão", "Venda nº " + sale.getSaleNumber(),
                        operator, SALE_REFERENCE_TYPE, sale.getId()));
                aggregateBalance = nextBalance;
            }
        }
        if (remaining > 0) {
            stockMovementRepository.save(new StockMovement(product, null, StockMovementType.COUNTER_SALE,
                    aggregateBalance, aggregateAfter, "Venda balcão", "Venda nº " + sale.getSaleNumber(),
                    operator, SALE_REFERENCE_TYPE, sale.getId()));
        }
    }
}
