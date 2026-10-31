package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.MarketplaceStockApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketplaceStockApplicationRepository extends JpaRepository<MarketplaceStockApplication, UUID> {
    Optional<MarketplaceStockApplication> findBySnapshotIdAndProductId(UUID snapshotId, UUID productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MarketplaceStockApplication a where a.snapshotId=:snapshotId and a.productId=:productId")
    Optional<MarketplaceStockApplication> findForUpdate(@Param("snapshotId") UUID snapshotId, @Param("productId") UUID productId);
    List<MarketplaceStockApplication> findByStatusOrderByUpdatedAtAsc(String status);
    boolean existsBySnapshotIdAndProductIdAndAppliedQuantityNot(UUID snapshotId,UUID productId,java.math.BigDecimal quantity);
}
