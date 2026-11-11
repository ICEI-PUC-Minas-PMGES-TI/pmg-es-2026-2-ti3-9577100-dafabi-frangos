package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_product_mapping", uniqueConstraints = @UniqueConstraint(
        name = "uk_marketplace_product_mapping",
        columnNames = {"provider", "external_store_id", "external_item_name", "product_id"}))
public class MarketplaceProductMapping {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private IntegrationProvider provider;
    @Column(name = "external_store_id", nullable = false, length = 120) private String externalStoreId;
    @Column(name = "external_item_name", nullable = false, length = 255) private String externalItemName;
    @Column(name = "product_id") private UUID productId;
    @Column(name = "quantity_per_item", precision = 12, scale = 3) private BigDecimal quantityPerItem;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected MarketplaceProductMapping() {}
    public MarketplaceProductMapping(IntegrationProvider provider, String storeId, String itemName,
            UUID productId, BigDecimal quantityPerItem) {
        this.id = UUID.randomUUID(); this.provider = provider; this.externalStoreId = storeId;
        this.externalItemName = itemName; this.productId = productId; this.quantityPerItem = quantityPerItem;
        this.updatedAt = OffsetDateTime.now();
    }
    public UUID getId(){return id;} public IntegrationProvider getProvider(){return provider;}
    public String getExternalStoreId(){return externalStoreId;} public String getExternalItemName(){return externalItemName;}
    public UUID getProductId(){return productId;} public BigDecimal getQuantityPerItem(){return quantityPerItem;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
