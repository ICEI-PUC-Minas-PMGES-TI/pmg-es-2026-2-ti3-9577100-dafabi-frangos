package com.dafabi.cash.domain;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_movement")
public class CashMovement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegister cashRegister;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashMovementDirection direction;

    @Column(name = "movement_type", nullable = false, length = 40)
    private String movementType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String origin;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected CashMovement() {
    }

    public CashMovement(CashRegister cashRegister, CashMovementDirection direction,
                        String movementType, BigDecimal amount, String origin,
                        String description, String referenceType, UUID referenceId) {
        this.id = UUID.randomUUID();
        this.cashRegister = cashRegister;
        this.direction = direction;
        this.movementType = movementType;
        this.amount = amount;
        this.origin = origin;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.occurredAt = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.occurredAt == null) {
            this.occurredAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public void setCashRegister(CashRegister cashRegister) {
        this.cashRegister = cashRegister;
    }

    public CashMovementDirection getDirection() {
        return direction;
    }

    public void setDirection(CashMovementDirection direction) {
        this.direction = direction;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
