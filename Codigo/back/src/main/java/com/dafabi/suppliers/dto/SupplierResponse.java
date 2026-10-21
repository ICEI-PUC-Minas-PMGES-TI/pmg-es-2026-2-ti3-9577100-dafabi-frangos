package com.dafabi.suppliers.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        UUID storeId,
        String name,
        String phone,
        String email,
        String notes,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
