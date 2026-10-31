package com.dafabi.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(BigDecimal revenue,BigDecimal expenses,BigDecimal ifoodFees,
        BigDecimal food99Fees,BigDecimal additionalMarketplaceCosts,BigDecimal marketplacePayouts,BigDecimal estimatedResult,int salesCount,int counterSales,int ifoodSales,
        int food99Sales,boolean financialDataComplete) {}
