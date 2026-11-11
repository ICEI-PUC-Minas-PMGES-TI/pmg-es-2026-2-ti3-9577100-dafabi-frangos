package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "integration_import_batch")
public class IntegrationImportBatch {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private IntegrationProvider provider;
    @Enumerated(EnumType.STRING) @Column(name = "report_type", nullable = false, length = 20) private IntegrationReportType reportType;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(name = "file_sha256", nullable = false, length = 64) private String fileSha256;
    @Column(name = "period_start") private LocalDate periodStart;
    @Column(name = "period_end") private LocalDate periodEnd;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ImportBatchStatus status;
    @Column(name = "row_count", nullable = false) private int rowCount;
    @Column(name = "valid_row_count", nullable = false) private int validRowCount;
    @Column(name = "invalid_row_count", nullable = false) private int invalidRowCount;
    @Column(name = "duplicate_row_count", nullable = false) private int duplicateRowCount;
    @Column(name = "summary_json", nullable = false, columnDefinition = "TEXT") private String summaryJson;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "imported_at") private OffsetDateTime importedAt;

    protected IntegrationImportBatch() {}

    public IntegrationImportBatch(UUID id, IntegrationProvider provider, IntegrationReportType reportType,
            String fileName, String fileSha256, LocalDate periodStart, LocalDate periodEnd,
            ImportBatchStatus status, int rowCount, int validRowCount, int invalidRowCount,
            int duplicateRowCount, String summaryJson, OffsetDateTime createdAt) {
        this.id=id; this.provider=provider; this.reportType=reportType; this.fileName=fileName;
        this.fileSha256=fileSha256; this.periodStart=periodStart; this.periodEnd=periodEnd;
        this.status=status; this.rowCount=rowCount; this.validRowCount=validRowCount;
        this.invalidRowCount=invalidRowCount; this.duplicateRowCount=duplicateRowCount;
        this.summaryJson=summaryJson; this.createdAt=createdAt;
    }

    public void markImported(ImportBatchStatus finalStatus, OffsetDateTime at) { this.status=finalStatus; this.importedAt=at; }
    public UUID getId(){return id;} public IntegrationProvider getProvider(){return provider;}
    public IntegrationReportType getReportType(){return reportType;} public String getFileName(){return fileName;}
    public String getFileSha256(){return fileSha256;} public LocalDate getPeriodStart(){return periodStart;}
    public LocalDate getPeriodEnd(){return periodEnd;} public ImportBatchStatus getStatus(){return status;}
    public int getRowCount(){return rowCount;} public int getValidRowCount(){return validRowCount;}
    public int getInvalidRowCount(){return invalidRowCount;} public int getDuplicateRowCount(){return duplicateRowCount;}
    public String getSummaryJson(){return summaryJson;} public OffsetDateTime getCreatedAt(){return createdAt;}
    public OffsetDateTime getImportedAt(){return importedAt;}
}
