package com.dafabi.integrations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketplaceSettlementRequest(@NotBlank String externalStoreId,@NotBlank @Size(max=160) String externalSettlementId,
        @NotNull LocalDate periodStart,@NotNull LocalDate periodEnd,BigDecimal grossAmount,BigDecimal feesAmount,
        BigDecimal adjustmentsAmount,BigDecimal retainedAmount,BigDecimal amountReceived,LocalDate payoutDate,
        @NotBlank String status,@Size(max=1000) String sourceReference) {}
