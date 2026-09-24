package com.dafabi.cash.controller;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.dto.CloseCashRequest;
import com.dafabi.cash.dto.CreateCashMovementRequest;
import com.dafabi.cash.dto.CashMovementResponse;
import com.dafabi.cash.dto.CashRegisterResponse;
import com.dafabi.cash.dto.CashRegisterSummaryResponse;
import com.dafabi.cash.dto.OpenCashRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/cash-registers")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    public CashRegisterController(CashRegisterService cashRegisterService) {
        this.cashRegisterService = cashRegisterService;
    }

    @PostMapping
    public ResponseEntity<CashRegisterResponse> openCashRegister(@Valid @RequestBody OpenCashRequest request) {
        CashRegisterResponse response = cashRegisterService.openCashRegister(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<CashRegisterResponse> closeCashRegister(
            @PathVariable UUID id,
            @Valid @RequestBody CloseCashRequest request) {
        return ResponseEntity.ok(cashRegisterService.closeCashRegister(id, request));
    }

    @PostMapping("/close")
    public ResponseEntity<CashRegisterResponse> closeCurrentCashRegister(
            @RequestParam(required = false) UUID storeId,
            @Valid @RequestBody CloseCashRequest request) {
        return ResponseEntity.ok(cashRegisterService.closeCurrentCashRegister(storeId, request));
    }

    @GetMapping("/current")
    public ResponseEntity<CashRegisterResponse> getCurrentCashRegister(@RequestParam(required = false) UUID storeId) {
        return cashRegisterService.findCurrent(storeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashRegisterResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(cashRegisterService.findById(id));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<CashRegisterSummaryResponse> getSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(cashRegisterService.getSummary(id));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<CashMovementResponse>> getMovements(@PathVariable UUID id) {
        return ResponseEntity.ok(cashRegisterService.getMovements(id));
    }

    @PostMapping("/{id}/movements")
    public ResponseEntity<CashMovementResponse> createMovement(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCashMovementRequest request) {
        CashMovementResponse response = cashRegisterService.createMovement(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
