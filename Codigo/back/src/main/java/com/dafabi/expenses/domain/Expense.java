package com.dafabi.expenses.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "expense")
public class Expense {

  @Id
  private UUID id;

  @NotBlank
  @Size(max = 255)
  @Column(nullable = false, length = 255)
  private String description;

  @NotBlank
  @Size(max = 100)
  @Column(nullable = false, length = 100)
  private String category;

  @NotNull
  @DecimalMin(value = "0.01")
  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Column(name = "expense_date", nullable = false)
  private LocalDate date;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 30)
  private ExpensePaymentMethod paymentMethod;

  @Size(max = 500)
  @Column(length = 500)
  private String observation;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExpenseStatus status = ExpenseStatus.PENDING;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public Expense() {
  }

  public Expense(UUID id, String description, String category, BigDecimal amount, LocalDate date,
      ExpensePaymentMethod paymentMethod, String observation, ExpenseStatus status,
      UUID userId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.description = description;
    this.category = category;
    this.amount = amount;
    this.date = date;
    this.paymentMethod = paymentMethod;
    this.observation = observation;
    this.status = status;
    this.userId = userId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  @PrePersist
  public void prePersist() {
    if (id == null)
      id = UUID.randomUUID();
    if (status == null)
      status = ExpenseStatus.PENDING;
    if (createdAt == null)
      createdAt = OffsetDateTime.now();
    if (updatedAt == null)
      updatedAt = createdAt;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public ExpensePaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(ExpensePaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getObservation() {
    return observation;
  }

  public void setObservation(String observation) {
    this.observation = observation;
  }

  public ExpenseStatus getStatus() {
    return status;
  }

  public void setStatus(ExpenseStatus status) {
    this.status = status;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}