package com.dafabi.sales.dto;

import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        Long saleNumber,
        UUID cashRegisterId,
        UUID operatorId,
        OffsetDateTime dateTime,
        SaleOrigin origin,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal total,
        SaleStatus status,
        List<SaleItemResponse> items
) {
}
