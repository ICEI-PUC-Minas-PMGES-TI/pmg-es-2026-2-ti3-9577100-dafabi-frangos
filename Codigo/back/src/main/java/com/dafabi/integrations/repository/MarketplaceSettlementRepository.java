package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface MarketplaceSettlementRepository extends JpaRepository<MarketplaceSettlement,UUID> {
    List<MarketplaceSettlement> findByProviderAndExternalStoreIdOrderByPeriodStartDesc(IntegrationProvider provider,String storeId);
    Optional<MarketplaceSettlement> findByProviderAndExternalStoreIdAndExternalSettlementId(IntegrationProvider provider,String storeId,String settlementId);
    List<MarketplaceSettlement> findByPayoutDateBetweenAndStatus(LocalDate start,LocalDate end,String status);
}
