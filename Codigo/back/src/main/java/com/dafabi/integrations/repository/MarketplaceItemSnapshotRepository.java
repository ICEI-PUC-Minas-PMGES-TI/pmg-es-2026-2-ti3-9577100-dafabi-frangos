package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceItemSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Collection;

public interface MarketplaceItemSnapshotRepository extends JpaRepository<MarketplaceItemSnapshot, UUID> {
    Optional<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndBusinessDateAndExternalItemName(
            IntegrationProvider provider, String storeId, LocalDate date, String itemName);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from MarketplaceItemSnapshot i where i.provider=:provider and i.externalStoreId=:store " +
           "and i.businessDate=:date and i.externalItemName=:name")
    Optional<MarketplaceItemSnapshot> findItemForUpdate(@Param("provider") IntegrationProvider provider,
            @Param("store") String storeId,@Param("date") LocalDate date,@Param("name") String itemName);
    List<MarketplaceItemSnapshot> findByProviderOrderByBusinessDateDesc(IntegrationProvider provider);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(
            IntegrationProvider provider, String storeId, LocalDate start, LocalDate end);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from MarketplaceItemSnapshot i where i.provider=:provider and i.externalStoreId=:store " +
           "and i.businessDate between :start and :end order by i.businessDate asc,i.externalItemName asc")
    List<MarketplaceItemSnapshot> findRangeForUpdate(@Param("provider") IntegrationProvider provider,
            @Param("store") String storeId,@Param("start") LocalDate start,@Param("end") LocalDate end);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdOrderByBusinessDateDesc(
            IntegrationProvider provider, String storeId);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndExternalItemNameIn(
            IntegrationProvider provider, String storeId, Collection<String> names);
    List<MarketplaceItemSnapshot> findByProviderAndExternalStoreIdAndExternalItemName(
            IntegrationProvider provider,String storeId,String itemName);
    long countByProvider(IntegrationProvider provider);
}
