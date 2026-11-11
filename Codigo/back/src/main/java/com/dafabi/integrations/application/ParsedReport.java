package com.dafabi.integrations.application;

import com.dafabi.integrations.domain.IntegrationReportType;
import com.dafabi.integrations.dto.ImportSummary;
import java.time.LocalDate;
import java.util.List;

public record ParsedReport(IntegrationReportType reportType, LocalDate start, LocalDate end,
        List<StagedReportRow> rows, ImportSummary summary) {
}
