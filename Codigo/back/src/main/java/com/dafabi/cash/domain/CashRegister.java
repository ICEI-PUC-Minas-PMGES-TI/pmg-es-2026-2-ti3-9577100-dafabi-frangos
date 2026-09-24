package com.dafabi.cash.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_register")
public class CashRegister {

    public static final UUID DEFAULT_STORE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(name = "operator_name", nullable = false, length = 150)
    private String operatorName;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "initial_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal initialBalance;

    @Column(name = "expected_balance_at_close", precision = 14, scale = 2)
    private BigDecimal expectedBalanceAtClose;

    @Column(name = "counted_balance", precision = 14, scale = 2)
    private BigDecimal countedBalance;

    @Column(name = "difference", precision = 14, scale = 2)
    private BigDecimal difference;

    @Column(length = 500)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashRegisterStatus status = CashRegisterStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CashMovement> movements = new ArrayList<>();

    public CashRegister() {
    }

    public CashRegister(UUID id, UUID storeId, UUID operatorId, String operatorName,
                        BigDecimal initialBalance, OffsetDateTime openedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.storeId = storeId != null ? storeId : DEFAULT_STORE_ID;
        this.operatorId = operatorId;
        this.operatorName = (operatorName != null && !operatorName.isBlank()) ? operatorName.trim() : "Operador";
        this.initialBalance = initialBalance;
        this.openedAt = openedAt != null ? openedAt : OffsetDateTime.now();
        this.status = CashRegisterStatus.OPEN;
        this.createdAt = this.openedAt;
        this.updatedAt = this.openedAt;
        this.version = 0L;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.storeId == null) {
            this.storeId = DEFAULT_STORE_ID;
        }
        if (this.openedAt == null) {
            this.openedAt = OffsetDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = this.openedAt;
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
        if (this.status == null) {
            this.status = CashRegisterStatus.OPEN;
        }
        if (this.operatorName == null || this.operatorName.isBlank()) {
            this.operatorName = "Operador";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void addMovement(CashMovement movement) {
        movements.add(movement);
        movement.setCashRegister(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(UUID operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getExpectedBalanceAtClose() {
        return expectedBalanceAtClose;
    }

    public void setExpectedBalanceAtClose(BigDecimal expectedBalanceAtClose) {
        this.expectedBalanceAtClose = expectedBalanceAtClose;
    }

    public BigDecimal getCountedBalance() {
        return countedBalance;
    }

    public void setCountedBalance(BigDecimal countedBalance) {
        this.countedBalance = countedBalance;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public CashRegisterStatus getStatus() {
        return status;
    }

    public void setStatus(CashRegisterStatus status) {
        this.status = status;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<CashMovement> getMovements() {
        return movements;
    }

    public void setMovements(List<CashMovement> movements) {
        this.movements = movements;
    }
}
