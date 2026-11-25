package com.dafabi.products.domain;

import com.dafabi.shared.Quantities;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_stock")
public class ProductStock {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = Quantities.ZERO;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public ProductStock() {
    }

    public ProductStock(Product product, Integer quantity) {
        this(product, quantity == null ? null : Quantities.of(quantity));
    }

    public ProductStock(Product product, BigDecimal quantity) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
        this.quantity = Quantities.of(quantity);
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
        if (this.quantity == null) {
            this.quantity = Quantities.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = Quantities.of(quantity);
    }

    public void setQuantity(int quantity) {
        this.quantity = Quantities.of(quantity);
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
