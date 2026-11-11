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
import org.springframework.security.test.context.support.WithMockUser;
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
@WithMockUser(roles = "ADMIN")
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
                .andExpect(status().isOk()).andExpect(jsonPath("$.expectedBalance", is(100.00)));

        JsonNode id = objectMapper.readTree(body).get("id");
        mockMvc.perform(patch("/expenses/{id}/cancel", id.asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CANCELLED")));
        mockMvc.perform(get("/cash-registers/{id}/summary", register.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.expectedBalance", is(100.00)));
    }

    @Test
    void shouldMoveCashAndConsolidateOnlyOnceWhenExpenseIsPaid() throws Exception {
        CashRegister register = cashRegisterRepository.save(new CashRegister(null, null, UUID.randomUUID(), "Fabiana Nogueira", new BigDecimal("100.00"), OffsetDateTime.now()));
        String body = mockMvc.perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Conta de energia","category":"Contas","amount":25.50,"date":"2026-10-27","paymentMethod":"CASH"}
                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(patch("/expenses/{id}/consolidate", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CONSOLIDATED")))
                .andExpect(jsonPath("$.paidAt").exists());
        mockMvc.perform(get("/cash-registers/{id}/summary", register.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cashExpenses", is(25.50)))
                .andExpect(jsonPath("$.expectedBalance", is(74.50)));
        mockMvc.perform(patch("/expenses/{id}/consolidate", id)).andExpect(status().isConflict());
        mockMvc.perform(patch("/expenses/{id}/cancel", id)).andExpect(status().isConflict());
    }

    @Test
    void shouldRejectCashExpenseWhenRegisterIsClosed() throws Exception {
        String body = mockMvc.perform(post("/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                {"description":"Compra de embalagens","category":"Materiais","amount":25.50,"date":"2026-10-27","paymentMethod":"CASH"}
                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(patch("/expenses/{id}/consolidate", objectMapper.readTree(body).get("id").asText()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code", is("CASH_CLOSED")));
    }
}
