package com.dafabi.products.mapper;

import com.dafabi.products.domain.Category;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.dto.CategoryResponse;
import com.dafabi.products.dto.CreateProductRequest;
import com.dafabi.products.dto.ProductListResponse;
import com.dafabi.products.dto.ProductResponse;
import com.dafabi.products.dto.UpdateProductRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request, Category category) {
        if (request == null) {
            return null;
        }

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setCategory(category);
        product.setName(request.name().trim());
        product.setSalePrice(request.salePrice());
        product.setCurrentCost(request.currentCost());
        product.setUnit(request.unit() != null && !request.unit().isBlank()
                ? request.unit().trim()
                : "UN");
        product.setBarcode(request.barcode() != null && !request.barcode().isBlank()
                ? request.barcode().trim()
                : null);
        product.setPerishable(Boolean.TRUE.equals(request.perishable()));
        product.setFrequent(Boolean.TRUE.equals(request.frequent()));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(OffsetDateTime.now());
        product.setUpdatedAt(product.getCreatedAt());
        product.setVersion(0L);

        return product;
    }

    public void updateEntity(Product product, UpdateProductRequest request, Category category) {
        product.setCategory(category);
        product.setName(request.name().trim());
        product.setSalePrice(request.salePrice());
        product.setCurrentCost(request.currentCost());
        product.setUnit(request.unit() != null && !request.unit().isBlank()
                ? request.unit().trim()
                : "UN");
        product.setBarcode(request.barcode() != null && !request.barcode().isBlank()
                ? request.barcode().trim()
                : null);
        product.setPerishable(Boolean.TRUE.equals(request.perishable()));
        product.setFrequent(Boolean.TRUE.equals(request.frequent()));
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        CategoryResponse categoryResponse = toCategoryResponse(product.getCategory());

        Integer stockQuantity = product.getStock() != null
                ? product.getStock().getQuantity()
                : 0;

        return new ProductResponse(
                product.getId(),
                categoryResponse,
                product.getName(),
                product.getSalePrice(),
                product.getCurrentCost(),
                product.getUnit(),
                product.getBarcode(),
                product.isPerishable(),
                product.isFrequent(),
                product.getStatus(),
                stockQuantity,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion());
    }

    public ProductListResponse toListResponse(Product product) {
        if (product == null) {
            return null;
        }

        Integer stockQuantity = product.getStock() != null
                ? product.getStock().getQuantity()
                : 0;

        return new ProductListResponse(
                product.getId(),
                product.getName(),
                toCategoryResponse(product.getCategory()),
                product.getSalePrice(),
                stockQuantity,
                product.getUnit(),
                product.getBarcode(),
                product.getStatus());
    }

    private CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }

        return new CategoryResponse(
                category.getId(),
                category.getName());
    }
}