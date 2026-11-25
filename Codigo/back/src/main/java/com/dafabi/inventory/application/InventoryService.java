package com.dafabi.inventory.application;

import com.dafabi.shared.Quantities;
import java.math.BigDecimal;
import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.dto.CreateInventoryLotRequest;
import com.dafabi.inventory.dto.InventoryLotResponse;
import com.dafabi.inventory.dto.InventoryPositionResponse;
import com.dafabi.inventory.dto.StockAdjustmentRequest;
import com.dafabi.inventory.dto.StockMovementResponse;
import com.dafabi.inventory.mapper.InventoryMapper;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.shared.security.RequestUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private static final String LOCAL_USER = "Usuário local";

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final InventoryMapper inventoryMapper;
    private final StockAvailabilityService stockAvailabilityService;

    public InventoryService(ProductRepository productRepository,
            ProductStockRepository productStockRepository,
            StockMovementRepository stockMovementRepository,
            InventoryLotRepository inventoryLotRepository,
            InventoryMapper inventoryMapper,
            StockAvailabilityService stockAvailabilityService) {
        this.productRepository = productRepository;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.inventoryMapper = inventoryMapper;
        this.stockAvailabilityService = stockAvailabilityService;
    }

    @Transactional(readOnly = true)
    public List<InventoryPositionResponse> findPositions(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<Product> products = productRepository.findAll().stream()
                .filter(product -> normalizedQuery.isBlank()
                        || product.getName().toLowerCase().contains(normalizedQuery)
                        || (product.getBarcode() != null && product.getBarcode().contains(normalizedQuery)))
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<UUID, StockMovement> lastMovements = lastMovementByProductId(
                products.stream().map(Product::getId).toList());

        Map<UUID, StockAvailability> availability = stockAvailabilityService.forProducts(products);
        return products.stream()
                .map(product -> inventoryMapper.toPositionResponse(product, lastMovements.get(product.getId()),
                        availability.get(product.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> findMovements(UUID productId) {
        if (productId != null && !productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Produto", productId);
        }
        List<StockMovement> movements = productId == null
                ? stockMovementRepository.findAll().stream()
                        .sorted(Comparator.comparing(StockMovement::getOccurredAt).reversed())
                        .toList()
                : stockMovementRepository.findByProduct_IdOrderByOccurredAtDesc(productId);
        return movements.stream().map(inventoryMapper::toMovementResponse).toList();
    }

    @Transactional
    public StockMovementResponse adjust(StockAdjustmentRequest request) {
        ProductStock stock = productStockRepository.findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        BigDecimal previousQuantity = stock.getQuantity();
        BigDecimal newQuantity = Quantities.of(request.newQuantity());
        if (previousQuantity.compareTo(newQuantity) == 0) {
            throw new BusinessException("UNCHANGED_STOCK", "A nova quantidade deve ser diferente da quantidade atual.");
        }

        stock.setQuantity(newQuantity);
        List<StockMovement> adjustments = new java.util.ArrayList<>();
        BigDecimal aggregateBalance = previousQuantity;
        BigDecimal reduction = previousQuantity.subtract(newQuantity);
        if (reduction.signum() > 0 && stock.getProduct().isPerishable()) {
            for (InventoryLot lot : inventoryLotRepository.findAvailableByProductForUpdate(stock.getProduct().getId())) {
                if (reduction.signum() == 0) break;
                BigDecimal amount = Quantities.min(reduction, lot.getQuantity());
                lot.setQuantity(lot.getQuantity().subtract(amount));
                BigDecimal next = aggregateBalance.subtract(amount);
                adjustments.add(stockMovementRepository.save(new StockMovement(
                        stock.getProduct(), lot, StockMovementType.MANUAL_ADJUSTMENT,
                        aggregateBalance, next, "Ajuste manual", request.reason().trim(),
                        actor(request.performedBy()), "STOCK_ADJUSTMENT", null)));
                aggregateBalance = next;
                reduction = reduction.subtract(amount);
            }
        }
        if (aggregateBalance.compareTo(newQuantity) != 0 || adjustments.isEmpty()) {
            adjustments.add(stockMovementRepository.save(new StockMovement(
                    stock.getProduct(), null, StockMovementType.MANUAL_ADJUSTMENT,
                    aggregateBalance, newQuantity, "Ajuste manual", request.reason().trim(),
                    actor(request.performedBy()), "STOCK_ADJUSTMENT", null)));
        }
        return inventoryMapper.toMovementResponse(adjustments.get(adjustments.size() - 1));
    }

    @Transactional(readOnly = true)
    public List<InventoryLotResponse> findLots() {
        return inventoryLotRepository.findAllByOrderByExpiryAsc().stream()
                .map(inventoryMapper::toLotResponse)
                .toList();
    }

    @Transactional
    public InventoryLotResponse assignExistingStockToLot(CreateInventoryLotRequest request, RequestUser user) {
        if (request.expiry().isBefore(request.receivedAt())) {
            throw new BusinessException("INVALID_EXPIRY", "A validade não pode ser anterior à data de entrada.");
        }
        ProductStock stock = productStockRepository.findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        Product product = stock.getProduct();
        if (!product.isPerishable()) {
            throw new BusinessException("PRODUCT_NOT_PERISHABLE", "O produto não é perecível.");
        }
        if (inventoryLotRepository.existsByProduct_IdAndBatchIgnoreCase(request.productId(), request.batch().trim())) {
            throw new BusinessException("DUPLICATE_BATCH", "Este lote já foi registrado para o produto informado.", HttpStatus.CONFLICT);
        }
        BigDecimal assigned = inventoryLotRepository.findAvailableByProductForUpdate(request.productId()).stream()
                .map(InventoryLot::getQuantity).reduce(Quantities.ZERO, BigDecimal::add);
        BigDecimal unassigned = stock.getQuantity().subtract(assigned);
        if (request.quantity().compareTo(unassigned) > 0) {
            throw new BusinessException("INSUFFICIENT_UNASSIGNED_STOCK",
                    "Quantidade superior ao saldo sem lote: " + Quantities.format(unassigned.max(BigDecimal.ZERO)) + ".", HttpStatus.CONFLICT);
        }
        InventoryLot lot = inventoryLotRepository.save(new InventoryLot(product, request.batch(),
                request.receivedAt(), request.expiry(), request.quantity()));
        stockMovementRepository.save(new StockMovement(product, lot, StockMovementType.LOT_ASSIGNMENT,
                stock.getQuantity(), stock.getQuantity(), "Regularização de lote",
                "Vinculadas " + Quantities.format(request.quantity()) + " unidades do saldo existente ao lote " + lot.getBatch(),
                actor(user.name()), "INVENTORY_LOT", lot.getId()));
        return inventoryMapper.toLotResponse(lot);
    }

    @Transactional
    public InventoryLotResponse createLot(CreateInventoryLotRequest request) {
        if (request.expiry().isBefore(request.receivedAt())) {
            throw new BusinessException("INVALID_EXPIRY", "A validade não pode ser anterior à data de entrada.");
        }
        if (request.expiry().isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) {
            throw new BusinessException("INVALID_EXPIRY", "A validade deve ser hoje ou uma data futura.");
        }
        if (inventoryLotRepository.existsByProduct_IdAndBatchIgnoreCase(request.productId(), request.batch().trim())) {
            throw new BusinessException("DUPLICATE_BATCH", "Este lote já foi registrado para o produto informado.",
                    HttpStatus.CONFLICT);
        }

        ProductStock stock = productStockRepository.findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        Product product = stock.getProduct();
        if (!product.isPerishable()) {
            throw new BusinessException("PRODUCT_NOT_PERISHABLE",
                    "O produto informado não está marcado como perecível.");
        }

        InventoryLot lot = inventoryLotRepository.save(new InventoryLot(
                product, request.batch(), request.receivedAt(), request.expiry(), request.quantity()));
        BigDecimal previousQuantity = stock.getQuantity();
        BigDecimal newQuantity = previousQuantity.add(Quantities.of(request.quantity()));
        stock.setQuantity(newQuantity);
        stockMovementRepository.save(new StockMovement(
                product, lot, StockMovementType.LOT_RECEIPT,
                previousQuantity, newQuantity, "Registro de lote",
                "Lote " + lot.getBatch() + " recebido", actor(request.performedBy()),
                "INVENTORY_LOT", lot.getId()));

        return inventoryMapper.toLotResponse(lot);
    }

    private Map<UUID, StockMovement> lastMovementByProductId(Collection<UUID> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        Map<UUID, StockMovement> lastMovements = new HashMap<>();
        for (StockMovement movement : stockMovementRepository.findByProduct_IdInOrderByOccurredAtDesc(productIds)) {
            lastMovements.putIfAbsent(movement.getProduct().getId(), movement);
        }
        return lastMovements;
    }

    private String actor(String performedBy) {
        return performedBy == null || performedBy.isBlank() ? LOCAL_USER : performedBy.trim();
    }
}
