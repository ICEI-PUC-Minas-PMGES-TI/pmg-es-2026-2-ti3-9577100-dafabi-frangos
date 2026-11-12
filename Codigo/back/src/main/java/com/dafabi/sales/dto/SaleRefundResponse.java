package com.dafabi.sales.dto;

import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SaleRefundResponse(
        UUID id,
        UUID saleId,
        Long saleNumber,
        SaleStatus saleStatus,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        BigDecimal cashAmount,
        UUID cashRegisterId,
        String reason,
        UUID refundedById,
        String refundedBy,
        OffsetDateTime refundedAt
) {
}
