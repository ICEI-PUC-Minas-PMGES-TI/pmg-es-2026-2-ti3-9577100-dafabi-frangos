package com.dafabi.dashboard.dto;

import java.util.List;

public record DashboardResponse(DashboardSummaryResponse summary,List<DashboardDailyResponse> daily,
        List<DashboardRecentSaleResponse> recentSales) {}
