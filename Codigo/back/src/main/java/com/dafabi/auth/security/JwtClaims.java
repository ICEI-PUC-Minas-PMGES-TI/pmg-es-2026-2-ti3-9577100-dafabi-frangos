package com.dafabi.auth.security;

import com.dafabi.shared.security.UserRole;

import java.time.Instant;
import java.util.UUID;

public record JwtClaims(
        UUID userId,
        String email,
        String name,
        UserRole role,
        Instant expiresAt
) {}
