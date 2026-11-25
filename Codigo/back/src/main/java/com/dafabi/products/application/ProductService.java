package com.dafabi.products.application;

import java.time.ZoneId;
import java.time.LocalDate;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.InventoryLot;
import java.util.Map;
import com.dafabi.inventory.application.StockAvailabilityService;
import com.dafabi.inventory.application.StockAvailability;
import com.dafabi.shared.Quantities;
import java.math.BigDecimal;
import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.dto.CreateProductRequest;
import com.dafabi.products.dto.ProductListResponse;
import com.dafabi.products.dto.ProductResponse;
import com.dafabi.products.dto.UpdateProductRequest;
import com.dafabi.products.mapper.ProductMapper;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    private static final ZoneId STORE_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final StockAvailabilityService stockAvailabilityService;
    private final InventoryLotRepository inventoryLotRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductMapper productMapper,
            StockAvailabilityService stockAvailabilityService,
            InventoryLotRepository inventoryLotRepository,
            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
        this.stockAvailabilityService = stockAvailabilityService;
        this.inventoryLotRepository = inventoryLotRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (request.barcode() != null && !request.barcode().isBlank()) {
            String trimmedBarcode = request.barcode().trim();

            if (productRepository.existsByBarcode(trimmedBarcode)) {
                throw new BusinessException(
                        "DUPLICATE_BARCODE",
                        "Já existe um produto cadastrado com este código de barras.");
            }
        }

        Category category = null;

        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));
        }

        Product product = productMapper.toEntity(request, category);

        BigDecimal initialQuantity = Quantities.of(request.initialStock());
        boolean needsInitialLot = product.isPerishable() && initialQuantity.signum() > 0;
        if (needsInitialLot) {
            validateInitialLot(request);
        }

        ProductStock stock = new ProductStock(product, initialQuantity);
        product.setStock(stock);

        Product saved = productRepository.save(product);

        if (initialQuantity.signum() > 0) {
            InventoryLot lot = null;
            if (needsInitialLot) {
                LocalDate today = LocalDate.now(STORE_ZONE);
                lot = new InventoryLot(saved, request.initialBatch().trim(), today, request.initialExpiry(), initialQuantity);
                inventoryLotRepository.save(lot);
            }
            stockMovementRepository.save(new StockMovement(saved, lot,
                    needsInitialLot ? StockMovementType.LOT_RECEIPT : StockMovementType.INITIAL_BALANCE,
                    Quantities.ZERO, initialQuantity, "Cadastro do produto",
                    needsInitialLot ? "Estoque inicial no lote " + lot.getBatch() : "Estoque inicial",
                    "Cadastro de produto", "PRODUCT", saved.getId()));
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findWithCategoryAndStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductListResponse> findAll(
            String query,
            UUID categoryId,
            ProductStatus status,
            boolean includeInactive,
            int page,
            int size) {
        if (page < 0) {
            throw new BusinessException(
                    "INVALID_PAGE",
                    "A página deve ser maior ou igual a zero.");
        }

        if (size <= 0) {
            throw new BusinessException(
                    "INVALID_SIZE",
                    "O tamanho da página deve ser maior que zero.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")));

        Specification<Product> specification = Specification.where(null);

        if (status != null) {
            specification = specification.and(
                    (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status));
        } else if (!includeInactive) {
            specification = specification.and(
                    (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));
        }

        if (categoryId != null) {
            specification = specification.and(
                    (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(
                            root.get("category").get("id"),
                            categoryId));
        }

        if (query != null && !query.isBlank()) {
            String search = "%" + query.trim().toLowerCase() + "%";

            specification = specification.and(
                    (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("name")),
                                    search),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("barcode")),
                                    search)));
        }

        Page<Product> products = productRepository.findAll(specification, pageable);
        Map<UUID, StockAvailability> availability = stockAvailabilityService.forProducts(products.getContent());
        return products.map(product -> productMapper.toListResponse(product, availability.get(product.getId())));
    }

    @Transactional(readOnly = true)
    public ProductResponse findByBarcode(String barcode) {
        String normalizedBarcode = normalizeBarcode(barcode);
        if (normalizedBarcode == null) {
            throw new BusinessException("INVALID_BARCODE", "Informe um código de barras válido.");
        }

        Product product = productRepository.findByBarcode(normalizedBarcode)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", normalizedBarcode));

        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findWithCategoryAndStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        String barcode = normalizeBarcode(request.barcode());

        if (barcode != null
                && productRepository.existsByBarcodeAndIdNot(barcode, id)) {
            throw new BusinessException(
                    "DUPLICATE_BARCODE",
                    "Já existe um produto cadastrado com este código de barras.",
                    HttpStatus.CONFLICT);
        }

        Category category = null;

        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));
        }

        BigDecimal currentStock = product.getStock() == null ? Quantities.ZERO : product.getStock().getQuantity();
        if (currentStock.signum() > 0 && request.currentCost() != null && product.getCurrentCost() != null
                && request.currentCost().compareTo(product.getCurrentCost()) != 0) {
            throw new BusinessException("COST_MANAGED_BY_PURCHASES",
                    "Com estoque disponível, o custo é o custo médio calculado pelas compras e não pode ser alterado à mão.");
        }

        productMapper.updateEntity(product, request, category);

        Product saved = productRepository.save(product);

        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findWithCategoryAndStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        product.setStatus(status);

        Product saved = productRepository.save(product);

        return toResponse(saved);
    }

    private void validateInitialLot(CreateProductRequest request) {
        if (request.initialBatch() == null || request.initialBatch().isBlank() || request.initialExpiry() == null) {
            throw new BusinessException("INITIAL_LOT_REQUIRED",
                    "Produto perecível com estoque inicial precisa de lote e validade.");
        }
        if (request.initialExpiry().isBefore(LocalDate.now(STORE_ZONE))) {
            throw new BusinessException("INVALID_EXPIRY", "A validade do lote inicial deve ser hoje ou uma data futura.");
        }
    }

    private ProductResponse toResponse(Product product) {
        return productMapper.toResponse(product, stockAvailabilityService.forProduct(product));
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }

        return barcode.trim();
    }
}
