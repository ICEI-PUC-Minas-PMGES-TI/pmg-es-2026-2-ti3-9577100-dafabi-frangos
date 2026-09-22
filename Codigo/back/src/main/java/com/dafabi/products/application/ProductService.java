package com.dafabi.products.application;

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

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
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

        int initialQuantity = request.initialStock() != null
                ? request.initialStock()
                : 0;

        ProductStock stock = new ProductStock(product, initialQuantity);
        product.setStock(stock);

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findWithCategoryAndStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductListResponse> findAll(
            String query,
            UUID categoryId,
            ProductStatus status,
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

        ProductStatus requestedStatus = status != null
                ? status
                : ProductStatus.ACTIVE;

        specification = specification.and(
                (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), requestedStatus));

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

        return productRepository.findAll(specification, pageable)
                .map(productMapper::toListResponse);
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

        productMapper.updateEntity(product, request, category);

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findWithCategoryAndStockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        product.setStatus(status);

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }

        return barcode.trim();
    }
}