package com.dafabi.expenses;

import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.expenses.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseControllerIT {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private CashMovementRepository cashMovementRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        cashMovementRepository.deleteAll();
        cashRegisterRepository.deleteAll();
    }

    @Test
    void shouldCreateCashExpenseAndReverseItOnCancel() throws Exception {
        CashRegister register = cashRegisterRepository.save(new CashRegister(null, null, UUID.randomUUID(), "Fabiana Nogueira", new BigDecimal("100.00"), OffsetDateTime.now()));
        String body = mockMvc.perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Compra de embalagens","category":"Materiais","amount":25.50,"date":"2026-10-27","paymentMethod":"CASH"}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status", is("PENDING"))).andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/cash-registers/{id}/summary", register.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expectedBalance", is(74.50)));

        JsonNode id = objectMapper.readTree(body).get("id");
        mockMvc.perform(patch("/expenses/{id}/cancel", id.asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CANCELLED")));
        mockMvc.perform(get("/cash-registers/{id}/summary", register.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expectedBalance", is(100.00)));
    }

    @Test
    void shouldRejectCashExpenseWhenRegisterIsClosed() throws Exception {
        mockMvc.perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Compra de embalagens","category":"Materiais","amount":25.50,"date":"2026-10-27","paymentMethod":"CASH"}
                """))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("CASH_CLOSED")));
    }
}
