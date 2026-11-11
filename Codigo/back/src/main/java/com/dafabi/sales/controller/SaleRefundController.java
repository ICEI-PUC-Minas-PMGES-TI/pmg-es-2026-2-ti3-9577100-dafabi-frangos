package com.dafabi.sales.controller;

import com.dafabi.sales.application.SaleRefundOutcome;
import com.dafabi.sales.application.SaleRefundService;
import com.dafabi.sales.dto.CreateSaleRefundRequest;
import com.dafabi.sales.dto.SaleRefundEligibilityResponse;
import com.dafabi.sales.dto.SaleRefundResponse;
import com.dafabi.shared.security.RequestUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/sales/{saleId}")
@PreAuthorize("hasRole('ADMIN')")
public class SaleRefundController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final SaleRefundService saleRefundService;

    public SaleRefundController(SaleRefundService saleRefundService) {
        this.saleRefundService = saleRefundService;
    }

    @GetMapping("/refund-eligibility")
    public ResponseEntity<SaleRefundEligibilityResponse> findEligibility(@PathVariable UUID saleId) {
        return ResponseEntity.ok(saleRefundService.findEligibility(saleId));
    }

    @GetMapping("/refund")
    public ResponseEntity<SaleRefundResponse> findRefund(@PathVariable UUID saleId) {
        return ResponseEntity.ok(saleRefundService.findBySaleId(saleId));
    }

    @PostMapping({"/refund", "/refunds"})
    public ResponseEntity<SaleRefundResponse> refund(
            @PathVariable UUID saleId,
            @Valid @RequestBody CreateSaleRefundRequest request,
            @RequestHeader(value = RequestUser.ID_HEADER, required = false) String userId,
            @RequestHeader(value = RequestUser.NAME_HEADER, required = false) String userName,
            @RequestHeader(value = RequestUser.ROLE_HEADER, required = false) String userRole,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        RequestUser user = RequestUser.resolve(userId, userName, userRole);
        SaleRefundOutcome outcome = saleRefundService.refund(saleId, request, user, idempotencyKey);

        if (!outcome.created()) {
            return ResponseEntity.ok(outcome.refund());
        }

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();
        return ResponseEntity.created(location).body(outcome.refund());
    }
}
