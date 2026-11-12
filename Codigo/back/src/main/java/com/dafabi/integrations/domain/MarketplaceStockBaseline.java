package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_stock_baseline", uniqueConstraints = @UniqueConstraint(
        name = "uk_marketplace_stock_baseline", columnNames = {"provider", "external_store_id"}))
public class MarketplaceStockBaseline {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private IntegrationProvider provider;
    @Column(name = "external_store_id", nullable = false, length = 120) private String externalStoreId;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    protected MarketplaceStockBaseline() {}
    public MarketplaceStockBaseline(IntegrationProvider provider, String storeId, LocalDate startDate) {
        this.id = UUID.randomUUID(); this.provider = provider; this.externalStoreId = storeId;
        this.startDate = startDate; this.updatedAt = OffsetDateTime.now();
    }
    public void setStartDate(LocalDate date){this.startDate=date;this.updatedAt=OffsetDateTime.now();}
    public UUID getId(){return id;} public IntegrationProvider getProvider(){return provider;}
    public String getExternalStoreId(){return externalStoreId;} public LocalDate getStartDate(){return startDate;}
}
