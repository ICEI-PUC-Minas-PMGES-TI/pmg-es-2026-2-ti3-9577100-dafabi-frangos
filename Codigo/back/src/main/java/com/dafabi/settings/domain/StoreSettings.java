package com.dafabi.settings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_settings")
public class StoreSettings {
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(nullable = false, length = 250)
    private String address;

    @Column(name = "default_payment", nullable = false, length = 20)
    private String defaultPayment;

    @Column(name = "compact_mode", nullable = false)
    private boolean compactMode;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StoreSettings() { }

    public StoreSettings(String name, String phone, String address, String defaultPayment, boolean compactMode) {
        this.id = DEFAULT_ID;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.defaultPayment = defaultPayment;
        this.compactMode = compactMode;
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(String name, String phone, String address, String defaultPayment, boolean compactMode) {
        this.name = name.trim();
        this.phone = phone.trim();
        this.address = address == null ? "" : address.trim();
        this.defaultPayment = defaultPayment;
        this.compactMode = compactMode;
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = OffsetDateTime.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDefaultPayment() { return defaultPayment; }
    public boolean isCompactMode() { return compactMode; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
