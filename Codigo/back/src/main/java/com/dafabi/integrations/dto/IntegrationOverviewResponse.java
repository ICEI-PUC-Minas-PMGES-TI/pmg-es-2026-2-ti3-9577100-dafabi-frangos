package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.time.OffsetDateTime;

public record IntegrationOverviewResponse(IntegrationProvider provider, OffsetDateTime lastImportedAt,
        long importedOrders, long itemSnapshots) {
}
