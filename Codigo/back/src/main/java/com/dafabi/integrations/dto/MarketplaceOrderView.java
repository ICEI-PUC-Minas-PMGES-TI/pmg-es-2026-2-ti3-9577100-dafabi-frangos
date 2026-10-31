package com.dafabi.integrations.dto;

import com.dafabi.integrations.domain.IntegrationProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketplaceOrderView(
        UUID id,
        IntegrationProvider provider,
        String externalStoreId,
        String externalOrderId,
        OffsetDateTime orderAt,
        String rawStatus,
        String normalizedStatus,
        BigDecimal grossItems,
        BigDecimal customerPaid,
        BigDecimal feesAndCommissions,
        BigDecimal netReported,
        String paymentMethodRaw,
        BigDecimal marketingCost,BigDecimal logisticsCost,BigDecimal refundAmount,
        BigDecimal freeDeliveryCost,BigDecimal tipAmount,BigDecimal originalItemCount,
        OffsetDateTime completedAt,OffsetDateTime cancelledAt,String cancellationResponsible,String cancellationReason) {
}
