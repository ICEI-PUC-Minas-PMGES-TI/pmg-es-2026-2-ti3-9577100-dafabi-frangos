package com.dafabi.purchases.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal total
) {
}
