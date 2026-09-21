package com.dafabi.products.application;

import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.dto.CreateProductRequest;
import com.dafabi.products.dto.ProductResponse;
import com.dafabi.products.mapper.ProductMapper;
import com.dafabi.products.repository.CategoryRepository;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
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
                throw new BusinessException("DUPLICATE_BARCODE", "Já existe um produto cadastrado com este código de barras.");
            }
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", request.categoryId()));
        }

        Product product = productMapper.toEntity(request, category);

        int initialQuantity = (request.initialStock() != null) ? request.initialStock() : 0;
        ProductStock stock = new ProductStock(product, initialQuantity);
        product.setStock(stock);

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        return productMapper.toResponse(product);
    }
}
