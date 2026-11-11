package com.dafabi.purchases.application;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.expenses.domain.ExpensePaymentMethod;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.purchases.domain.Purchase;
import com.dafabi.purchases.domain.PurchaseItem;
import com.dafabi.purchases.domain.PurchaseStatus;
import com.dafabi.purchases.domain.PurchasePaymentStatus;
import com.dafabi.purchases.dto.CreatePurchaseRequest;
import com.dafabi.purchases.dto.PayPurchaseRequest;
import com.dafabi.purchases.dto.PurchaseItemRequest;
import com.dafabi.purchases.dto.PurchaseResponse;
import com.dafabi.purchases.dto.PurchaseSummaryResponse;
import com.dafabi.purchases.mapper.PurchaseMapper;
import com.dafabi.purchases.repository.PurchaseRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.suppliers.domain.Supplier;
import com.dafabi.suppliers.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class PurchaseService {

    private static final String PURCHASE_REFERENCE_TYPE = "PURCHASE";
    private static final String PURCHASE_ORIGIN = "Compra";
    private static final String LOCAL_USER = "Usuário local";

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PurchaseMapper purchaseMapper;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterService cashRegisterService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           ProductStockRepository productStockRepository,
                           StockMovementRepository stockMovementRepository,
                           PurchaseMapper purchaseMapper,
                           CashRegisterRepository cashRegisterRepository,
                           CashRegisterService cashRegisterService) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.purchaseMapper = purchaseMapper;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashRegisterService = cashRegisterService;
    }

    @Transactional
    public PurchaseResponse create(CreatePurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_SUPPLIER",
                        "Fornecedor não encontrado com identificador: " + request.supplierId()));
        if (!supplier.isActive()) {
            throw new BusinessException(
                    "SUPPLIER_INACTIVE",
                    String.format("O fornecedor \"%s\" está inativo e não pode receber compras.", supplier.getName()));
        }

        String actor = actor(request.performedBy());
        Purchase purchase = new Purchase(
                UUID.randomUUID(),
                supplier,
                request.date(),
                null,
                PurchaseStatus.REGISTERED,
                actor,
                null);

        // Ordem fixa de bloqueio dos saldos evita deadlock entre operações concorrentes.
        Map<UUID, Integer> quantitiesByProduct = new TreeMap<>(Comparator.comparing(UUID::toString));
        for (PurchaseItemRequest item : request.items()) {
            quantitiesByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Map<UUID, Product> productsById = new HashMap<>();
        Map<UUID, CostChange> costsByProduct = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : quantitiesByProduct.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            ProductStock stock = productStockRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException(
                            "INVALID_PRODUCT",
                            "Produto não encontrado com identificador: " + productId));
            Product product = stock.getProduct();

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException(
                        "PRODUCT_INACTIVE",
                        String.format("O produto \"%s\" está inativo e não pode ser comprado.", product.getName()));
            }

            int previousQuantity = stock.getQuantity();
            int newQuantity = previousQuantity + quantity;
            BigDecimal previousCost = money(product.getCurrentCost());
            BigDecimal totalInventoryCost = previousCost.multiply(BigDecimal.valueOf(previousQuantity))
                    .add(request.items().stream().filter(item -> item.productId().equals(productId))
                            .map(item -> money(item.unitCost()).multiply(BigDecimal.valueOf(item.quantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal resultingCost = totalInventoryCost.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
            product.setCurrentCost(resultingCost);
            stock.setQuantity(newQuantity);
            stockMovementRepository.save(new StockMovement(
                    product, null, StockMovementType.PURCHASE,
                    previousQuantity, newQuantity, PURCHASE_ORIGIN,
                    "Compra registrada de " + supplier.getName(), actor,
                    PURCHASE_REFERENCE_TYPE, purchase.getId()));
            productsById.put(productId, product);
            costsByProduct.put(productId, new CostChange(previousCost, resultingCost));
        }

        for (PurchaseItemRequest item : request.items()) {
            CostChange costs = costsByProduct.get(item.productId());
            purchase.addItem(new PurchaseItem(productsById.get(item.productId()), item.quantity(), item.unitCost(),
                    costs.previous(), costs.resulting()));
        }

        Purchase saved = purchaseRepository.save(purchase);
        return purchaseMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PurchaseSummaryResponse> findAll(UUID supplierId) {
        List<Purchase> purchases = supplierId == null
                ? purchaseRepository.findAllByOrderByPurchaseDateDescCreatedAtDesc()
                : purchaseRepository.findBySupplier_IdOrderByPurchaseDateDescCreatedAtDesc(supplierId);
        return purchases.stream()
                .map(purchaseMapper::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse findById(UUID id) {
        return purchaseRepository.findWithDetailsById(id)
                .map(purchaseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", id));
    }

    @Transactional
    public PurchaseResponse pay(UUID id, PayPurchaseRequest request) {
        Purchase purchase = purchaseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", id));
        if (purchase.getStatus() != PurchaseStatus.REGISTERED
                || purchase.getPaymentStatus() != PurchasePaymentStatus.PENDING) {
            throw new BusinessException("PURCHASE_NOT_PAYABLE",
                    "Somente compras registradas e pendentes podem ser pagas.", HttpStatus.CONFLICT);
        }

        if (request.paymentMethod() == ExpensePaymentMethod.CASH) {
            CashRegister register = cashRegisterRepository.findByStoreIdAndStatus(
                            CashRegister.DEFAULT_STORE_ID, CashRegisterStatus.OPEN)
                    .orElseThrow(() -> new BusinessException("CASH_CLOSED",
                            "Abra o caixa antes de registrar o pagamento em dinheiro.", HttpStatus.CONFLICT));
            cashRegisterService.recordMovement(register.getId(), CashMovementDirection.OUT,
                    "PURCHASE", purchase.getTotalAmount(), "Pagamento de compra",
                    "Compra para " + purchase.getSupplier().getName(), "PURCHASE", purchase.getId());
        }

        purchase.setPaymentStatus(PurchasePaymentStatus.PAID);
        purchase.setPaymentMethod(request.paymentMethod());
        purchase.setPaidAt(OffsetDateTime.now());
        return purchaseMapper.toResponse(purchaseRepository.save(purchase));
    }

    @Transactional
    public PurchaseResponse cancel(UUID id) {
        Purchase purchase = purchaseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", id));
        if (purchase.getStatus() != PurchaseStatus.REGISTERED
                || purchase.getPaymentStatus() != PurchasePaymentStatus.PENDING) {
            throw new BusinessException("PURCHASE_NOT_CANCELLABLE",
                    "Somente compras registradas e não pagas podem ser canceladas.", HttpStatus.CONFLICT);
        }

        Map<UUID, Integer> quantitiesByProduct = new TreeMap<>(Comparator.comparing(UUID::toString));
        purchase.getItems().forEach(item -> quantitiesByProduct.merge(
                item.getProduct().getId(), item.getQuantity(), Integer::sum));
        for (Map.Entry<UUID, Integer> entry : quantitiesByProduct.entrySet()) {
            ProductStock stock = productStockRepository.findByProductIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", entry.getKey()));
            if (stock.getQuantity() < entry.getValue()) {
                throw new BusinessException("PURCHASE_STOCK_ALREADY_CONSUMED",
                        "Não é possível cancelar: parte do estoque desta compra já foi consumida.", HttpStatus.CONFLICT);
            }
            if (stockMovementRepository.existsByProduct_IdAndReferenceTypeAndOccurredAtAfter(
                    entry.getKey(), PURCHASE_REFERENCE_TYPE, purchase.getCreatedAt())) {
                throw new BusinessException("PURCHASE_COST_CHANGED",
                        "Não é possível cancelar: há compras posteriores que alteraram o custo médio do produto.",
                        HttpStatus.CONFLICT);
            }
        }

        for (Map.Entry<UUID, Integer> entry : quantitiesByProduct.entrySet()) {
            ProductStock stock = productStockRepository.findByProductIdForUpdate(entry.getKey()).orElseThrow();
            int previousQuantity = stock.getQuantity();
            int newQuantity = previousQuantity - entry.getValue();
            stock.setQuantity(newQuantity);
            PurchaseItem costSource = purchase.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(entry.getKey())).findFirst().orElseThrow();
            if (money(stock.getProduct().getCurrentCost()).compareTo(money(costSource.getResultingProductCost())) != 0) {
                throw new BusinessException("PURCHASE_COST_CHANGED",
                        "Não é possível cancelar: o custo médio do produto foi alterado depois desta compra.",
                        HttpStatus.CONFLICT);
            }
            stock.getProduct().setCurrentCost(money(costSource.getPreviousProductCost()));
            stockMovementRepository.save(new StockMovement(stock.getProduct(), null,
                    StockMovementType.PURCHASE_CANCELLATION, previousQuantity, newQuantity,
                    "Cancelamento de compra", "Cancelamento da compra " + purchase.getId(),
                    actor(purchase.getCreatedBy()), PURCHASE_REFERENCE_TYPE, purchase.getId()));
        }
        purchase.setStatus(PurchaseStatus.CANCELED);
        return purchaseMapper.toResponse(purchaseRepository.save(purchase));
    }

    private String actor(String performedBy) {
        return performedBy == null || performedBy.isBlank() ? LOCAL_USER : performedBy.trim();
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private record CostChange(BigDecimal previous, BigDecimal resulting) { }
}
