package com.dafabi.integrations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MarketplaceStockBaselineRequest(@NotBlank String externalStoreId, @NotNull LocalDate startDate) {}
