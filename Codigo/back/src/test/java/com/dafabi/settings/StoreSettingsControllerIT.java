package com.dafabi.settings;

import com.dafabi.settings.domain.StoreSettings;
import com.dafabi.settings.repository.StoreSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreSettingsControllerIT {
    @Autowired private MockMvc mockMvc;
    @Autowired private StoreSettingsRepository repository;

    @BeforeEach
    void ensureDefaultSettings() {
        repository.findById(StoreSettings.DEFAULT_ID).orElseGet(() -> repository.save(new StoreSettings(
                "DaFabi Frangos", "(31) 99123-4567", "Rua das Palmeiras, 125 — Belo Horizonte/MG", "PIX", false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReadAndPersistStoreSettings() throws Exception {
        mockMvc.perform(get("/settings/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("DaFabi Frangos")));

        mockMvc.perform(put("/settings/store").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"DaFabi Centro","phone":"(31) 99999-0000","address":"Rua Nova, 10",
                 "defaultPayment":"CASH","compactMode":true}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("DaFabi Centro")))
                .andExpect(jsonPath("$.defaultPayment", is("CASH")))
                .andExpect(jsonPath("$.compactMode", is(true)));

        var persisted = repository.findById(StoreSettings.DEFAULT_ID).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getPhone()).isEqualTo("(31) 99999-0000");
        persisted.update("DaFabi Frangos", "(31) 99123-4567", "Rua das Palmeiras, 125 — Belo Horizonte/MG", "PIX", false);
        repository.save(persisted);
    }

    @Test
    @WithMockUser(roles = "CASHIER")
    void shouldDenyStoreSettingsToCashier() throws Exception {
        mockMvc.perform(get("/settings/store")).andExpect(status().isForbidden());
    }
}
