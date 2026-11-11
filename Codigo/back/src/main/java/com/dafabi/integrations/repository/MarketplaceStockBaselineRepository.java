package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceStockBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MarketplaceStockBaselineRepository extends JpaRepository<MarketplaceStockBaseline, UUID> {
    Optional<MarketplaceStockBaseline> findByProviderAndExternalStoreId(IntegrationProvider provider, String storeId);
}
