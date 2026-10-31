package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationImportRow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface IntegrationImportRowRepository extends JpaRepository<IntegrationImportRow, UUID> {
    List<IntegrationImportRow> findByBatchIdOrderByRowNumber(UUID batchId);
    void deleteByBatchId(UUID batchId);
}
