package com.dafabi.suppliers.controller;

import com.dafabi.suppliers.application.SupplierService;
import com.dafabi.suppliers.dto.CreateSupplierRequest;
import com.dafabi.suppliers.dto.SupplierResponse;
import com.dafabi.suppliers.dto.UpdateSupplierRequest;
import com.dafabi.suppliers.dto.UpdateSupplierStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse response = supplierService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> findAll(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(supplierService.findAll(query, active, storeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SupplierResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierStatusRequest request) {
        return ResponseEntity.ok(supplierService.updateStatus(id, request.active()));
    }
}
