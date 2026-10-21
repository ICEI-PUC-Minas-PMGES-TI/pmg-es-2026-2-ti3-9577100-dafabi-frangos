package com.dafabi.suppliers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier")
public class Supplier {

    public static final UUID DEFAULT_STORE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Supplier() {
    }

    public Supplier(UUID id, UUID storeId, String name, String phone, String email,
                    String notes, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.storeId = storeId != null ? storeId : DEFAULT_STORE_ID;
        this.name = name != null ? name.trim() : null;
        this.phone = phone != null ? phone.trim() : null;
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.notes = (notes != null && !notes.isBlank()) ? notes.trim() : null;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
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
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.notes != null && this.notes.isBlank()) {
            this.notes = null;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.notes != null && this.notes.isBlank()) {
            this.notes = null;
        }
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null && !notes.isBlank()) ? notes.trim() : null;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
