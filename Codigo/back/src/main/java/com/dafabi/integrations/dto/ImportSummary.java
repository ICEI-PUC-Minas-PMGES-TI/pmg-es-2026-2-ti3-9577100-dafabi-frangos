package com.dafabi.integrations.dto;

import java.math.BigDecimal;

public record ImportSummary(
        int records,
        int completedOrders,
        int canceledOrders,
        int partiallyCanceledOrders,
        BigDecimal grossItems,
        BigDecimal customerPaid,
        BigDecimal itemRevenue,
        BigDecimal itemQuantity,
        BigDecimal ifoodPromotions,
        BigDecimal storePromotions,
        BigDecimal serviceFees,
        BigDecimal feesAndCommissions,
        BigDecimal netReported,
        int duplicates,
        int invalidRows) {
}
