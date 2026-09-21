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
        Integer stockQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
) {
}
