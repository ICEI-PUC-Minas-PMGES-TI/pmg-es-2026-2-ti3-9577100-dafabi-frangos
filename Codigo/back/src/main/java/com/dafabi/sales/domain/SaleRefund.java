package com.dafabi.sales.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sale_refund")
public class SaleRefund {

  @Id
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false, unique = true)
  private Sale sale;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(name = "cash_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal cashAmount;

  @Column(name = "cash_register_id")
  private UUID cashRegisterId;

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(name = "refunded_by_id", nullable = false)
  private UUID refundedById;

  @Column(name = "refunded_by_name", nullable = false, length = 150)
  private String refundedByName;

  @Column(name = "idempotency_key", length = 100, unique = true)
  private String idempotencyKey;

  @Column(name = "refunded_at", nullable = false)
  private OffsetDateTime refundedAt;

  public SaleRefund() {
  }

  public SaleRefund(UUID id, Sale sale, BigDecimal amount, BigDecimal cashAmount,
      UUID cashRegisterId, String reason, UUID refundedById,
      String refundedByName, String idempotencyKey) {
    this.id = id;
    this.sale = sale;
    this.amount = amount;
    this.cashAmount = cashAmount;
    this.cashRegisterId = cashRegisterId;
    this.reason = reason;
    this.refundedById = refundedById;
    this.refundedByName = refundedByName;
    this.idempotencyKey = idempotencyKey;
    this.refundedAt = OffsetDateTime.now();
  }

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }

    if (this.refundedAt == null) {
      this.refundedAt = OffsetDateTime.now();
    }

    if (this.cashAmount == null) {
      this.cashAmount = BigDecimal.ZERO;
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Sale getSale() {
    return sale;
  }

  public void setSale(Sale sale) {
    this.sale = sale;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getCashAmount() {
    return cashAmount;
  }

  public void setCashAmount(BigDecimal cashAmount) {
    this.cashAmount = cashAmount;
  }

  public UUID getCashRegisterId() {
    return cashRegisterId;
  }

  public void setCashRegisterId(UUID cashRegisterId) {
    this.cashRegisterId = cashRegisterId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public UUID getRefundedById() {
    return refundedById;
  }

  public void setRefundedById(UUID refundedById) {
    this.refundedById = refundedById;
  }

  public String getRefundedByName() {
    return refundedByName;
  }

  public void setRefundedByName(String refundedByName) {
    this.refundedByName = refundedByName;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public OffsetDateTime getRefundedAt() {
    return refundedAt;
  }

  public void setRefundedAt(OffsetDateTime refundedAt) {
    this.refundedAt = refundedAt;
  }
}
