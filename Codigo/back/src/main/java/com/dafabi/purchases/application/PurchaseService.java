package com.dafabi.purchases.application;

import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.purchases.domain.Purchase;
import com.dafabi.purchases.domain.PurchaseItem;
import com.dafabi.purchases.domain.PurchaseStatus;
import com.dafabi.purchases.dto.CreatePurchaseRequest;
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

    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           ProductStockRepository productStockRepository,
                           StockMovementRepository stockMovementRepository,
                           PurchaseMapper purchaseMapper) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.purchaseMapper = purchaseMapper;
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
            stock.setQuantity(newQuantity);
            stockMovementRepository.save(new StockMovement(
                    product, null, StockMovementType.PURCHASE,
                    previousQuantity, newQuantity, PURCHASE_ORIGIN,
                    "Compra registrada de " + supplier.getName(), actor,
                    PURCHASE_REFERENCE_TYPE, purchase.getId()));
            productsById.put(productId, product);
        }

        for (PurchaseItemRequest item : request.items()) {
            purchase.addItem(new PurchaseItem(productsById.get(item.productId()), item.quantity(), item.unitCost()));
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

    private String actor(String performedBy) {
        return performedBy == null || performedBy.isBlank() ? LOCAL_USER : performedBy.trim();
    }
}
