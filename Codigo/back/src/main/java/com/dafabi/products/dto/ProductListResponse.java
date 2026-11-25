package com.dafabi.products.dto;

import com.dafabi.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductListResponse(
    UUID id,
    String name,
    CategoryResponse category,
    BigDecimal salePrice,
    BigDecimal currentCost,
    BigDecimal stockQuantity,
    String unit,
    String barcode,
    boolean perishable,
    boolean frequent,
    ProductStatus status,
    BigDecimal sellableQuantity,
    BigDecimal expiredQuantity,
    BigDecimal untrackedQuantity) {
}
