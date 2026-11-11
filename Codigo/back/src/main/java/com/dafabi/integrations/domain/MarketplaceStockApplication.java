package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_stock_application", uniqueConstraints = @UniqueConstraint(
        name = "uk_marketplace_stock_application", columnNames = {"snapshot_id", "product_id"}))
public class MarketplaceStockApplication {
    @Id private UUID id;
    @Column(name = "snapshot_id", nullable = false) private UUID snapshotId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "reported_quantity", nullable = false, precision = 14, scale = 3) private BigDecimal reportedQuantity;
    @Column(name = "applied_quantity", nullable = false, precision = 14, scale = 3) private BigDecimal appliedQuantity;
    @Column(nullable = false, length = 32) private String status;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    protected MarketplaceStockApplication() {}
    public MarketplaceStockApplication(UUID snapshotId, UUID productId, BigDecimal reportedQuantity, String status) {
        this.id=UUID.randomUUID(); this.snapshotId=snapshotId; this.productId=productId;
        this.reportedQuantity=reportedQuantity; this.appliedQuantity=BigDecimal.ZERO; this.status=status;
        this.updatedAt=OffsetDateTime.now();
    }
    public void updateReported(BigDecimal value,String nextStatus){this.reportedQuantity=value;this.status=nextStatus;this.updatedAt=OffsetDateTime.now();}
    public void markApplied(BigDecimal value){this.appliedQuantity=value;this.reportedQuantity=value;this.status="APPLIED";this.updatedAt=OffsetDateTime.now();}
    public void setStatus(String value){this.status=value;this.updatedAt=OffsetDateTime.now();}
    public UUID getId(){return id;} public UUID getSnapshotId(){return snapshotId;} public UUID getProductId(){return productId;}
    public BigDecimal getReportedQuantity(){return reportedQuantity;} public BigDecimal getAppliedQuantity(){return appliedQuantity;}
    public String getStatus(){return status;}
}
