package com.dafabi.integrations.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MarketplaceMappingComponentView(UUID productId, String productName, String unit,
        BigDecimal quantityPerItem) {}
