package com.dafabi.auth.dto;

import com.dafabi.shared.security.UserRole;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        List<String> permissions
) {}
