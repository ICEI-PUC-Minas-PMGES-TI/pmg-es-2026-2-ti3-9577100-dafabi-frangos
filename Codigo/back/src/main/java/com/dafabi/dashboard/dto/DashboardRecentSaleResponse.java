package com.dafabi.dashboard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardRecentSaleResponse(String id,String number,OffsetDateTime createdAt,String origin,
        BigDecimal net,String status) {}
