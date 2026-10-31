package com.dafabi.integrations.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ParsedItemRow(
        String externalStoreId, String storeName, LocalDate businessDate,
        String externalItemName, String normalizedItemName, BigDecimal itemRevenue,
        BigDecimal soldQuantity, Map<String, String> sourceValues) {
}
