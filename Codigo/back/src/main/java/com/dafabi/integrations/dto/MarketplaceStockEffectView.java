package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MarketplaceStockEffectView(UUID snapshotId, IntegrationProvider provider, String externalStoreId,
        LocalDate businessDate, String externalItemName, UUID productId, String productName,
        BigDecimal reportedQuantity, BigDecimal appliedQuantity, BigDecimal quantityToApply,
        String status, String explanation) {}
