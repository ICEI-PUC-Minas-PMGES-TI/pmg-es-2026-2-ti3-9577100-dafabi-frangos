package com.dafabi.purchases.domain;

import com.dafabi.products.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "purchase_item")
public class PurchaseItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 255)
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitCost;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    public PurchaseItem() {
    }

    public PurchaseItem(Product product, Integer quantity, BigDecimal unitCost) {
        this(
                null,
                null,
                product,
                product != null ? product.getName() : null,
                quantity,
                unitCost,
                calculateLineTotal(quantity, unitCost)
        );
    }

    public PurchaseItem(UUID id, Purchase purchase, Product product, String productName,
                        Integer quantity, BigDecimal unitCost, BigDecimal totalAmount) {
        this.id = id;
        this.purchase = purchase;
        this.product = product;
        this.productName = (productName != null && !productName.isBlank())
                ? productName.trim()
                : (product != null ? product.getName() : null);
        this.quantity = quantity;
        this.unitCost = unitCost != null ? unitCost.setScale(2, RoundingMode.HALF_UP) : null;
        this.totalAmount = totalAmount != null
                ? totalAmount.setScale(2, RoundingMode.HALF_UP)
                : calculateLineTotal(quantity, unitCost);
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if ((this.productName == null || this.productName.isBlank()) && this.product != null) {
            this.productName = this.product.getName();
        }
        if (this.totalAmount == null && this.quantity != null && this.unitCost != null) {
            this.totalAmount = calculateLineTotal(this.quantity, this.unitCost);
        }
    }

    private static BigDecimal calculateLineTotal(Integer quantity, BigDecimal unitCost) {
        if (quantity == null || unitCost == null) {
            return null;
        }
        return unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if ((this.productName == null || this.productName.isBlank()) && product != null) {
            this.productName = product.getName();
        }
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = (productName != null && !productName.isBlank()) ? productName.trim() : null;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost != null ? unitCost.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : null;
    }
}
