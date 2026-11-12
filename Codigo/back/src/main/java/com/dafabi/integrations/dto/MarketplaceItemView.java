package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MarketplaceItemView(
        UUID id,
        IntegrationProvider provider,
        String externalStoreId,
        LocalDate date,
        String externalItemName,
        BigDecimal revenue,
        BigDecimal quantity) {
}
