package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.time.LocalDate;

public record MarketplaceStockBaselineView(IntegrationProvider provider,String externalStoreId,LocalDate startDate) {}
