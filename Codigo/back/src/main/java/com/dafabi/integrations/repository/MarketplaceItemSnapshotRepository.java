package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceItemSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Collection;

public interface MarketplaceItemSnapshotRepository extends JpaRepository<MarketplaceItemSnapshot, UUID> {
    Optional<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndBusinessDateAndExternalItemName(
            IntegrationProvider provider, String storeId, LocalDate date, String itemName);
    List<MarketplaceItemSnapshot> findByProviderOrderByBusinessDateDesc(IntegrationProvider provider);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(
            IntegrationProvider provider, String storeId, LocalDate start, LocalDate end);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdOrderByBusinessDateDesc(
            IntegrationProvider provider, String storeId);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndExternalItemNameIn(
            IntegrationProvider provider, String storeId, Collection<String> names);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndExternalItemName(
            IntegrationProvider provider,String storeId,String itemName);
    long countByProvider(IntegrationProvider provider);
}
