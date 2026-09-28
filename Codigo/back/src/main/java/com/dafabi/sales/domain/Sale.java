package com.dafabi.sales.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sale")
public class Sale {

  @Id
  private UUID id;

  @Column(name = "cash_register_id")
  private UUID cashRegisterId;

  @Column(name = "operator_id", nullable = false)
  private UUID operatorId;

  @Column(name = "sale_number", nullable = false)
  private Long saleNumber;

  @Column(name = "date_time", nullable = false)
  private OffsetDateTime dateTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SaleOrigin origin;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal subtotal;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SaleStatus status;

  @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SaleItem> items = new ArrayList<>();

  public Sale() {
  }

  public Sale(UUID id, UUID cashRegisterId, UUID operatorId, Long saleNumber,
      OffsetDateTime dateTime, SaleOrigin origin,
      PaymentMethod paymentMethod, BigDecimal subtotal,
      BigDecimal total, SaleStatus status) {
    this.id = id;
    this.cashRegisterId = cashRegisterId;
    this.operatorId = operatorId;
    this.saleNumber = saleNumber;
    this.dateTime = dateTime;
    this.origin = origin;
    this.paymentMethod = paymentMethod;
    this.subtotal = subtotal;
    this.total = total;
    this.status = status;
  }

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }

    if (this.dateTime == null) {
      this.dateTime = OffsetDateTime.now();
    }

    if (this.status == null) {
      this.status = SaleStatus.COMPLETED;
    }
  }

  public void addItem(SaleItem item) {
    this.items.add(item);
    item.setSale(this);
  }

  public void removeItem(SaleItem item) {
    this.items.remove(item);
    item.setSale(null);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getCashRegisterId() {
    return cashRegisterId;
  }

  public void setCashRegisterId(UUID cashRegisterId) {
    this.cashRegisterId = cashRegisterId;
  }

  public UUID getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(UUID operatorId) {
    this.operatorId = operatorId;
  }

  public Long getSaleNumber() {
    return saleNumber;
  }

  public void setSaleNumber(Long saleNumber) {
    this.saleNumber = saleNumber;
  }

  public OffsetDateTime getDateTime() {
    return dateTime;
  }

  public void setDateTime(OffsetDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public SaleOrigin getOrigin() {
    return origin;
  }

  public void setOrigin(SaleOrigin origin) {
    this.origin = origin;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public SaleStatus getStatus() {
    return status;
  }

  public void setStatus(SaleStatus status) {
    this.status = status;
  }

  public List<SaleItem> getItems() {
    return items;
  }

  public void setItems(List<SaleItem> items) {
    this.items = items;
  }
}
