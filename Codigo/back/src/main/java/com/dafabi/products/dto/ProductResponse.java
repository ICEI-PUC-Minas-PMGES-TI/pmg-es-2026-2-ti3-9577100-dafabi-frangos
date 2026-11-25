package com.dafabi.products.dto;

import com.dafabi.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        CategoryResponse category,
        String name,
        BigDecimal salePrice,
        BigDecimal currentCost,
        String unit,
        String barcode,
        boolean perishable,
        boolean frequent,
        ProductStatus status,
        BigDecimal stockQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version,
        BigDecimal sellableQuantity,
        BigDecimal expiredQuantity,
        BigDecimal untrackedQuantity
) {
    public ProductResponse withoutCost() {
        return new ProductResponse(id, category, name, salePrice, null, unit, barcode, perishable, frequent,
                status, stockQuantity, createdAt, updatedAt, version, sellableQuantity, expiredQuantity,
                untrackedQuantity);
    }
}
