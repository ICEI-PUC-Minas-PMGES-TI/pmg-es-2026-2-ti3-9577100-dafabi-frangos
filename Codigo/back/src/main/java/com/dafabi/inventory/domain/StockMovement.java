package com.dafabi.inventory.domain;

import com.dafabi.products.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private StockMovementType movementType;

    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(nullable = false, length = 100)
    private String origin;

    @Column(length = 500)
    private String reason;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    protected StockMovement() {
    }

    public StockMovement(Product product, InventoryLot lot, StockMovementType movementType,
            int previousQuantity, int newQuantity, String origin, String reason, String performedBy,
            String referenceType, UUID referenceId) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.lot = lot;
        this.occurredAt = OffsetDateTime.now();
        this.movementType = movementType;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.quantityDelta = newQuantity - previousQuantity;
        this.origin = origin;
        this.reason = reason;
        this.performedBy = performedBy;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public InventoryLot getLot() { return lot; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public StockMovementType getMovementType() { return movementType; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public Integer getQuantityDelta() { return quantityDelta; }
    public String getOrigin() { return origin; }
    public String getReason() { return reason; }
    public String getPerformedBy() { return performedBy; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
}
