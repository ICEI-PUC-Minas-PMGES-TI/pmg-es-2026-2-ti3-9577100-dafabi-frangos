package com.dafabi.settings.dto;

import com.dafabi.settings.domain.StoreSettings;

import java.time.OffsetDateTime;

public record StoreSettingsResponse(
        String name,
        String phone,
        String address,
        String defaultPayment,
        boolean compactMode,
        OffsetDateTime updatedAt
) {
    public static StoreSettingsResponse from(StoreSettings settings) {
        return new StoreSettingsResponse(settings.getName(), settings.getPhone(), settings.getAddress(),
                settings.getDefaultPayment(), settings.isCompactMode(), settings.getUpdatedAt());
    }
}
