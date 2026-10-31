package com.dafabi.dashboard.controller;

import com.dafabi.dashboard.application.DashboardService;
import com.dafabi.dashboard.dto.DashboardResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service){this.service=service;}
    @GetMapping
    public ResponseEntity<DashboardResponse> summary(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate){
        return ResponseEntity.ok(service.summary(startDate,endDate));
    }
}
