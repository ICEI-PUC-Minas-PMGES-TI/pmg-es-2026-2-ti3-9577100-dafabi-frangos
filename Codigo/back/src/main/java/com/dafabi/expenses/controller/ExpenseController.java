package com.dafabi.expenses.controller;

import com.dafabi.expenses.application.ExpenseService;
import com.dafabi.expenses.domain.ExpensePaymentMethod;
import com.dafabi.expenses.domain.ExpenseStatus;
import com.dafabi.expenses.dto.CreateExpenseRequest;
import com.dafabi.expenses.dto.ExpenseResponse;
import com.dafabi.expenses.dto.UpdateExpenseRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
    private final ExpenseService service;

    public ExpenseController(ExpenseService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<ExpenseResponse> findAll(@RequestParam(required = false) LocalDate startDate,
                                         @RequestParam(required = false) LocalDate endDate,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) ExpensePaymentMethod paymentMethod,
                                         @RequestParam(required = false) ExpenseStatus status) {
        return service.findAll(startDate, endDate, category, paymentMethod, status);
    }

    @GetMapping("/{id}")
    public ExpenseResponse findById(@PathVariable UUID id) { return service.findById(id); }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateExpenseRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public ExpenseResponse cancel(@PathVariable UUID id) { return service.cancel(id); }
}
