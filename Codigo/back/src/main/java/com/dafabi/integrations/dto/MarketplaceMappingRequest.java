package com.dafabi.integrations.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MarketplaceMappingRequest(boolean ignored, @Valid List<Component> components) {
    public record Component(@NotNull UUID productId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantityPerItem) {}
}
