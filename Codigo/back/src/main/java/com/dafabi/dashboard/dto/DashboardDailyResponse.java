package com.dafabi.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardDailyResponse(LocalDate date,String day,BigDecimal value) {}
