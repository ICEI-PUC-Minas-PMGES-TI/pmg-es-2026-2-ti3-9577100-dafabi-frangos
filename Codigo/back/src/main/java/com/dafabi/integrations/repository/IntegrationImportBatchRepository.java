package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationImportBatch;
import com.dafabi.integrations.domain.IntegrationProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationImportBatchRepository extends JpaRepository<IntegrationImportBatch, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from IntegrationImportBatch b where b.id = :id")
    Optional<IntegrationImportBatch> findByIdForUpdate(@Param("id") UUID id);
    Optional<IntegrationImportBatch> findByProviderAndFileSha256(IntegrationProvider provider, String fileSha256);
    List<IntegrationImportBatch> findTop50ByProviderOrderByCreatedAtDesc(IntegrationProvider provider);
}
