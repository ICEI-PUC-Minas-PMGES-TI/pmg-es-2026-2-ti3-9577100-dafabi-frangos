package com.dafabi.users.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tb_roles")
public class Role {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // Ex: ROLE_ADMIN, ROLE_OPERATOR

    protected Role() {}

    public Role(UUID id, String name) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name == null ? null : name.trim().toUpperCase();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
