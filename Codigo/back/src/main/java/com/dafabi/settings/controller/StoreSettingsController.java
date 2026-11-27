package com.dafabi.settings.controller;

import com.dafabi.settings.application.StoreSettingsService;
import com.dafabi.settings.dto.StoreSettingsResponse;
import com.dafabi.settings.dto.UpdateStoreSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/store")
public class StoreSettingsController {
    private final StoreSettingsService service;

    public StoreSettingsController(StoreSettingsService service) { this.service = service; }

    /** Leitura liberada ao Operador de caixa: o PDV usa a forma de pagamento sugerida. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public StoreSettingsResponse get() { return service.get(); }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StoreSettingsResponse update(@Valid @RequestBody UpdateStoreSettingsRequest request) {
        return service.update(request);
    }
}
