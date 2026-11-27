package com.dafabi.users.dto;

import com.dafabi.shared.security.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 100) String name,
        @NotNull(message = "Informe o perfil.") UserRole role
) {}
