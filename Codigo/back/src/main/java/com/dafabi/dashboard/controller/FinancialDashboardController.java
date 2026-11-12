package com.dafabi.dashboard.controller;

import com.dafabi.dashboard.application.FinancialDashboardService;
import com.dafabi.dashboard.dto.FinancialDashboardResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class FinancialDashboardController {
    private final FinancialDashboardService service;

    public FinancialDashboardController(FinancialDashboardService service) {
        this.service = service;
    }

    @GetMapping({"", "/financial-summary"})
    public ResponseEntity<FinancialDashboardResponse> getFinancialSummary(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.getSummary(period, startDate, endDate));
    }
}
