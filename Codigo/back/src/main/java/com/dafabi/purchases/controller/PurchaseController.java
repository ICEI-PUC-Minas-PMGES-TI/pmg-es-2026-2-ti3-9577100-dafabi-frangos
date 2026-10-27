package com.dafabi.purchases.controller;

import com.dafabi.purchases.application.PurchaseService;
import com.dafabi.purchases.dto.CreatePurchaseRequest;
import com.dafabi.purchases.dto.PurchaseResponse;
import com.dafabi.purchases.dto.PurchaseSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody CreatePurchaseRequest request) {
        PurchaseResponse response = purchaseService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseSummaryResponse>> findAll(@RequestParam(required = false) UUID supplierId) {
        return ResponseEntity.ok(purchaseService.findAll(supplierId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.findById(id));
    }
}
