package com.dafabi.settings.application;

import com.dafabi.settings.domain.StoreSettings;
import com.dafabi.settings.dto.StoreSettingsResponse;
import com.dafabi.settings.dto.UpdateStoreSettingsRequest;
import com.dafabi.settings.repository.StoreSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreSettingsService {
    private final StoreSettingsRepository repository;

    public StoreSettingsService(StoreSettingsRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public StoreSettingsResponse get() {
        return repository.findById(StoreSettings.DEFAULT_ID).map(StoreSettingsResponse::from)
                .orElseThrow(() -> new IllegalStateException("Configurações padrão da loja não foram inicializadas."));
    }

    @Transactional
    public StoreSettingsResponse update(UpdateStoreSettingsRequest request) {
        StoreSettings settings = repository.findById(StoreSettings.DEFAULT_ID)
                .orElseThrow(() -> new IllegalStateException("Configurações padrão da loja não foram inicializadas."));
        settings.update(request.name(), request.phone(), request.address(), request.defaultPayment(), request.compactMode());
        return StoreSettingsResponse.from(repository.save(settings));
    }
}
