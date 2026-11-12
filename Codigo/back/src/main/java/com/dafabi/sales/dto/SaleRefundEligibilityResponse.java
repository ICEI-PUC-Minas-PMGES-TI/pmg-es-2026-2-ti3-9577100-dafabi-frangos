package com.dafabi.sales.dto;

import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleRefundEligibilityResponse(
        UUID saleId,
        Long saleNumber,
        OffsetDateTime dateTime,
        SaleOrigin origin,
        PaymentMethod paymentMethod,
        SaleStatus status,
        BigDecimal total,
        BigDecimal cashAmountToReverse,
        boolean eligible,
        String ineligibilityCode,
        String ineligibilityReason,
        List<SaleItemResponse> items,
        SaleRefundResponse refund
) {
}
