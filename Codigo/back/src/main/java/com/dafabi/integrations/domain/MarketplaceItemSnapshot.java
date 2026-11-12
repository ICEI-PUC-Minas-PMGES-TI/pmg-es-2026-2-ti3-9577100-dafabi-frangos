package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="marketplace_item_snapshot", uniqueConstraints=@UniqueConstraint(name="uk_marketplace_item_snapshot",columnNames={"provider","external_store_id","business_date","external_item_name"}))
public class MarketplaceItemSnapshot {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private IntegrationProvider provider;
    @Column(name="external_store_id",nullable=false,length=120) private String externalStoreId;
    @Column(name="store_name") private String storeName;
    @Column(name="business_date",nullable=false) private LocalDate businessDate;
    @Column(name="external_item_name",nullable=false) private String externalItemName;
    @Column(name="normalized_item_name",nullable=false) private String normalizedItemName;
    @Column(name="item_revenue",nullable=false,precision=14,scale=2) private BigDecimal itemRevenue;
    @Column(name="sold_quantity",nullable=false,precision=14,scale=3) private BigDecimal soldQuantity;
    @Column(name="source_batch_id",nullable=false) private UUID sourceBatchId;
    @Column(name="source_payload_json",nullable=false,columnDefinition="TEXT") private String sourcePayloadJson;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected MarketplaceItemSnapshot() {}
    public MarketplaceItemSnapshot(UUID id,IntegrationProvider provider,String externalStoreId,String storeName,
            LocalDate businessDate,String externalItemName,String normalizedItemName,BigDecimal itemRevenue,
            BigDecimal soldQuantity,UUID sourceBatchId,String sourcePayloadJson,OffsetDateTime updatedAt){
        this.id=id;this.provider=provider;this.externalStoreId=externalStoreId;this.storeName=storeName;
        this.businessDate=businessDate;this.externalItemName=externalItemName;this.normalizedItemName=normalizedItemName;
        this.itemRevenue=itemRevenue;this.soldQuantity=soldQuantity;this.sourceBatchId=sourceBatchId;
        this.sourcePayloadJson=sourcePayloadJson;this.updatedAt=updatedAt;
    }
    public void replaceFrom(MarketplaceItemSnapshot o){itemRevenue=o.itemRevenue;soldQuantity=o.soldQuantity;storeName=o.storeName;sourceBatchId=o.sourceBatchId;sourcePayloadJson=o.sourcePayloadJson;updatedAt=o.updatedAt;}
    public UUID getId(){return id;} public IntegrationProvider getProvider(){return provider;} public String getExternalStoreId(){return externalStoreId;}
    public LocalDate getBusinessDate(){return businessDate;} public String getExternalItemName(){return externalItemName;}
    public BigDecimal getItemRevenue(){return itemRevenue;} public BigDecimal getSoldQuantity(){return soldQuantity;}
}
