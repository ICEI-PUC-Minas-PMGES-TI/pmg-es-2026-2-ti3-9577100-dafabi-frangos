package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "integration_import_row")
public class IntegrationImportRow {
    @Id private UUID id;
    @Column(name = "batch_id", nullable = false) private UUID batchId;
    @Column(name = "row_number", nullable = false) private int rowNumber;
    @Column(name = "row_key") private String rowKey;
    @Column(nullable = false) private boolean valid;
    @Column(name = "duplicate_in_file", nullable = false) private boolean duplicateInFile;
    @Column(name = "duplicate_in_system", nullable = false) private boolean duplicateInSystem;
    @Column(name = "issues_json", nullable = false, columnDefinition = "TEXT") private String issuesJson;
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT") private String payloadJson;

    protected IntegrationImportRow() {}
    public IntegrationImportRow(UUID id, UUID batchId, int rowNumber, String rowKey, boolean valid,
            boolean duplicateInFile, boolean duplicateInSystem, String issuesJson, String payloadJson) {
        this.id=id; this.batchId=batchId; this.rowNumber=rowNumber; this.rowKey=rowKey;
        this.valid=valid; this.duplicateInFile=duplicateInFile; this.duplicateInSystem=duplicateInSystem;
        this.issuesJson=issuesJson; this.payloadJson=payloadJson;
    }
    public UUID getId(){return id;} public UUID getBatchId(){return batchId;} public int getRowNumber(){return rowNumber;}
    public String getRowKey(){return rowKey;} public boolean isValid(){return valid;}
    public boolean isDuplicateInFile(){return duplicateInFile;} public boolean isDuplicateInSystem(){return duplicateInSystem;}
    public String getIssuesJson(){return issuesJson;} public String getPayloadJson(){return payloadJson;}
}
