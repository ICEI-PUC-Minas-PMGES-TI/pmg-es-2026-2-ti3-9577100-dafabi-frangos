package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketplaceSettlementView(UUID id,IntegrationProvider provider,String externalStoreId,String externalSettlementId,
        LocalDate periodStart,LocalDate periodEnd,BigDecimal grossAmount,BigDecimal feesAmount,BigDecimal adjustmentsAmount,
        BigDecimal retainedAmount,BigDecimal amountReceived,LocalDate payoutDate,String status,String sourceReference,OffsetDateTime updatedAt) {}
