package com.dafabi.purchases.mapper;

import com.dafabi.purchases.domain.Purchase;
import com.dafabi.purchases.domain.PurchaseItem;
import com.dafabi.purchases.dto.PurchaseItemResponse;
import com.dafabi.purchases.dto.PurchaseResponse;
import com.dafabi.purchases.dto.PurchaseSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PurchaseMapper {

    public PurchaseResponse toResponse(Purchase purchase) {
        if (purchase == null) return null;

        return new PurchaseResponse(
                purchase.getId(),
                purchase.getSupplier().getId(),
                purchase.getSupplier().getName(),
                purchase.getPurchaseDate(),
                purchase.getItems().size(),
                normalize(purchase.getTotalAmount()),
                purchase.getStatus(),
                purchase.getCreatedBy(),
                purchase.getCreatedAt(),
                purchase.getItems().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    public PurchaseSummaryResponse toSummaryResponse(Purchase purchase) {
        if (purchase == null) return null;

        return new PurchaseSummaryResponse(
                purchase.getId(),
                purchase.getSupplier().getId(),
                purchase.getSupplier().getName(),
                purchase.getPurchaseDate(),
                purchase.getItems().size(),
                normalize(purchase.getTotalAmount()),
                purchase.getStatus(),
                purchase.getCreatedAt()
        );
    }

    public PurchaseItemResponse toItemResponse(PurchaseItem item) {
        if (item == null) return null;

        return new PurchaseItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getQuantity(),
                normalize(item.getUnitCost()),
                normalize(item.getTotalAmount())
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
