package com.dafabi.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record FinancialDashboardResponse(
        LocalDate startDate,
        LocalDate endDate,
        Summary summary,
        Map<String, Long> salesByOrigin,
        List<DailyRevenue> daily,
        List<RecentSale> recentSales) {

    public record Summary(
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal costOfGoodsSold,
            BigDecimal fees,
            BigDecimal ifoodFees,
            BigDecimal food99Fees,
            BigDecimal otherMarketplaceCosts,
            boolean marketplaceFeesComplete,
            BigDecimal estimatedResult,
            long salesCount,
            long counterSales,
            long ifoodSales,
            long food99Sales,
            BigDecimal cashIn,
            BigDecimal cashOut) {
    }

    public record DailyRevenue(LocalDate date, BigDecimal revenue) {
    }

    public record RecentSale(String id, String number, OffsetDateTime dateTime, String origin, BigDecimal total, String status) {
    }
}
