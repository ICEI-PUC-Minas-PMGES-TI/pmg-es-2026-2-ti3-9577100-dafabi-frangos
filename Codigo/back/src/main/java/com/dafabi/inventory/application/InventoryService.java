package com.dafabi.inventory.application;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final String LOCAL_USER = "Usuário local";

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(ProductRepository productRepository,
            ProductStockRepository productStockRepository,
            StockMovementRepository stockMovementRepository,
            InventoryLotRepository inventoryLotRepository,
            InventoryMapper inventoryMapper) {
        this.productRepository = productRepository;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.inventoryMapper = inventoryMapper;
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

        return products.stream()
                .map(product -> inventoryMapper.toPositionResponse(product, lastMovements.get(product.getId())))
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
        int previousQuantity = stock.getQuantity();
        int newQuantity = request.newQuantity();
        if (previousQuantity == newQuantity) {
            throw new BusinessException("UNCHANGED_STOCK", "A nova quantidade deve ser diferente da quantidade atual.");
        }

        stock.setQuantity(newQuantity);
        StockMovement movement = new StockMovement(
                stock.getProduct(), null, StockMovementType.MANUAL_ADJUSTMENT,
                previousQuantity, newQuantity, "Ajuste manual", request.reason().trim(),
                actor(request.performedBy()), "STOCK_ADJUSTMENT", null);
        stockMovementRepository.save(movement);
        return inventoryMapper.toMovementResponse(movement);
    }

    @Transactional(readOnly = true)
    public List<InventoryLotResponse> findLots() {
        return inventoryLotRepository.findAllByOrderByExpiryAsc().stream()
                .map(inventoryMapper::toLotResponse)
                .toList();
    }

    @Transactional
    public InventoryLotResponse createLot(CreateInventoryLotRequest request) {
        if (request.expiry().isBefore(request.receivedAt())) {
            throw new BusinessException("INVALID_EXPIRY", "A validade não pode ser anterior à data de entrada.");
        }
        if (request.expiry().isBefore(LocalDate.now())) {
            throw new BusinessException("INVALID_EXPIRY", "A validade deve ser hoje ou uma data futura.");
        }
        if (inventoryLotRepository.existsByProduct_IdAndBatchIgnoreCase(request.productId(), request.batch().trim())) {
            throw new BusinessException("DUPLICATE_BATCH", "Este lote já foi registrado para o produto informado.", HttpStatus.CONFLICT);
        }

        ProductStock stock = productStockRepository.findByProductIdForUpdate(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));
        Product product = stock.getProduct();
        if (!product.isPerishable()) {
            throw new BusinessException("PRODUCT_NOT_PERISHABLE", "O produto informado não está marcado como perecível.");
        }

        InventoryLot lot = inventoryLotRepository.save(new InventoryLot(
                product, request.batch(), request.receivedAt(), request.expiry(), request.quantity()));
        int previousQuantity = stock.getQuantity();
        int newQuantity = previousQuantity + request.quantity();
        stock.setQuantity(newQuantity);
        stockMovementRepository.save(new StockMovement(
                product, lot, StockMovementType.LOT_RECEIPT,
                previousQuantity, newQuantity, "Registro de lote",
                "Lote " + lot.getBatch() + " recebido", actor(request.performedBy()),
                "INVENTORY_LOT", lot.getId()));

        return inventoryMapper.toLotResponse(lot);
    }

    private Map<UUID, StockMovement> lastMovementByProductId(Collection<UUID> productIds) {
        if (productIds.isEmpty()) return Map.of();
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
