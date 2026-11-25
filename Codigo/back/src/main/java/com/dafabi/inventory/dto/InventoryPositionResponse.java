package com.dafabi.inventory.dto;

import com.dafabi.products.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryPositionResponse(
        UUID productId,
        String productName,
        String categoryName,
        BigDecimal stockQuantity,
        String unit,
        BigDecimal currentCost,
        boolean perishable,
        ProductStatus productStatus,
        StockMovementResponse lastMovement,
        BigDecimal sellableQuantity,
        BigDecimal expiredQuantity,
        BigDecimal untrackedQuantity
) {
}
