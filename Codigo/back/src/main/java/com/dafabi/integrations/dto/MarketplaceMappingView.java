package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.util.List;

public record MarketplaceMappingView(IntegrationProvider provider, String externalStoreId,
        String externalItemName, boolean ignored, List<MarketplaceMappingComponentView> components) {}
