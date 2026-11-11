package com.dafabi.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String identifier,
        String email,
        String username,
        @NotBlank(message = "A senha é obrigatória.")
        String password
) {
    public String resolvedIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return "";
    }
}
