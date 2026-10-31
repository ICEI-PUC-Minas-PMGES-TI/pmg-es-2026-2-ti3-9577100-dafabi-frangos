package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.ImportBatchStatus;
import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.IntegrationReportType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ImportBatchResponse(
        UUID id,
        IntegrationProvider provider,
        IntegrationReportType reportType,
        String fileName,
        LocalDate periodStart,
        LocalDate periodEnd,
        ImportBatchStatus status,
        int rowCount,
        int validRowCount,
        int invalidRowCount,
        int duplicateRowCount,
        ImportSummary summary,
        List<ImportRowPreview> rows,
        OffsetDateTime createdAt,
        OffsetDateTime importedAt,
        boolean alreadyImported) {
}
