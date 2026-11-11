package com.dafabi.settings.repository;

import com.dafabi.settings.domain.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, UUID> { }
