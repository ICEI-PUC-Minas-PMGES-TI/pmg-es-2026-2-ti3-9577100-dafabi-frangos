package com.dafabi.integrations.repository;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.Collection;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, UUID> {
    boolean existsByProviderAndExternalStoreIdAndExternalOrderId(IntegrationProvider provider, String storeId, String orderId);
    Optional<MarketplaceOrder> findByProviderAndExternalStoreIdAndExternalOrderId(IntegrationProvider provider, String storeId, String orderId);
    List<MarketplaceOrder> findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThanOrderByOrderAtDesc(
            IntegrationProvider provider, OffsetDateTime start, OffsetDateTime endExclusive);
    long countByProvider(IntegrationProvider provider);
    List<MarketplaceOrder> findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThan(
            IntegrationProvider provider, OffsetDateTime start, OffsetDateTime endExclusive);
}
