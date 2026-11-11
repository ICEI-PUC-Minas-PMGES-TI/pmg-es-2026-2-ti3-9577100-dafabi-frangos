package com.dafabi.users.dto;

import com.dafabi.shared.security.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, UserRole role, boolean active, OffsetDateTime createdAt) {}
