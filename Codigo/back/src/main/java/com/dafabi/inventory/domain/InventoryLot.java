package com.dafabi.inventory.domain;

import com.dafabi.products.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_lot")
public class InventoryLot {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String batch;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Column(nullable = false)
    private LocalDate expiry;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected InventoryLot() {
    }

    public InventoryLot(Product product, String batch, LocalDate receivedAt, LocalDate expiry, Integer quantity) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.batch = batch.trim();
        this.receivedAt = receivedAt;
        this.expiry = expiry;
        this.quantity = quantity;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public String getBatch() { return batch; }
    public LocalDate getReceivedAt() { return receivedAt; }
    public LocalDate getExpiry() { return expiry; }
    public Integer getQuantity() { return quantity; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
