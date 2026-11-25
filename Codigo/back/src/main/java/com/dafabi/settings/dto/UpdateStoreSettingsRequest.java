package com.dafabi.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStoreSettingsRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) @Pattern(regexp = "^\\(?\\d{2}\\)?[ -]?9?\\d{4}-?\\d{4}$", message = "Informe o telefone com DDD, ex.: (31) 99999-9999.") String phone,
        @Size(max = 250) String address,
        @NotBlank @Pattern(regexp = "CASH|PIX|DEBIT|CREDIT") String defaultPayment,
        @NotNull Boolean compactMode
) { }
