package com.dafabi.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FinancialDashboardResponse(
        LocalDate startDate,
        LocalDate endDate,
        Summary summary,
        Map<String, Long> salesByOrigin,
        List<DailyRevenue> daily) {

    public record Summary(
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal fees,
            BigDecimal ifoodFees,
            BigDecimal food99Fees,
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
}
