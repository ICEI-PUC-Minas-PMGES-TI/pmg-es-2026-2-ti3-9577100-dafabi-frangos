package com.dafabi.products.dto;

import com.dafabi.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductListResponse(
    UUID id,
    String name,
    CategoryResponse category,
    BigDecimal salePrice,
    Integer stockQuantity,
    String unit,
    String barcode,
    ProductStatus status) {
}
