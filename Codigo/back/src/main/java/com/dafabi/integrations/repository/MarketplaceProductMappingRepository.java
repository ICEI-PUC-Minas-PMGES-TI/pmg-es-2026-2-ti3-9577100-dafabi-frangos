package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MarketplaceProductMappingRepository extends JpaRepository<MarketplaceProductMapping, UUID> {
    List<MarketplaceProductMapping> findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(
            IntegrationProvider provider, String storeId, String itemName);
    List<MarketplaceProductMapping> findByProviderAndExternalStoreIdAndExternalItemNameIn(
            IntegrationProvider provider, String storeId, List<String> itemNames);
    void deleteByProviderAndExternalStoreIdAndExternalItemName(
            IntegrationProvider provider, String storeId, String itemName);
}
