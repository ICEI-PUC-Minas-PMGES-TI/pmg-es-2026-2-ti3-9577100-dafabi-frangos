package com.dafabi.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordRequest(
        @NotBlank(message = "Informe a senha atual.") String currentPassword,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres.") String newPassword
) {}
