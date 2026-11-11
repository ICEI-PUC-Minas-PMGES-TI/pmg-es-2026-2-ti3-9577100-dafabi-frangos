package com.dafabi.sales.controller;

import com.dafabi.sales.application.SaleRefundService;
import com.dafabi.sales.dto.SaleRefundResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/refunds")
@PreAuthorize("hasRole('ADMIN')")
public class SaleRefundHistoryController {

    private final SaleRefundService saleRefundService;

    public SaleRefundHistoryController(SaleRefundService saleRefundService) {
        this.saleRefundService = saleRefundService;
    }

    @GetMapping
    public Page<SaleRefundResponse> findAll(Pageable pageable) {
        return saleRefundService.findAll(pageable);
    }

    @GetMapping("/{refundId}")
    public SaleRefundResponse findById(@PathVariable UUID refundId) {
        return saleRefundService.findById(refundId);
    }
}
