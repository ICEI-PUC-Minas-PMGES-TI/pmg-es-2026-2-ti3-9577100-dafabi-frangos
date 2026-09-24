package com.dafabi.cash;

import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.dto.CloseCashRequest;
import com.dafabi.cash.dto.CreateCashMovementRequest;
import com.dafabi.cash.dto.OpenCashRequest;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CashRegisterControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private CashMovementRepository cashMovementRepository;

    @BeforeEach
    void setUp() {
        cashMovementRepository.deleteAll();
        cashRegisterRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /cash-registers deve abrir caixa com sucesso")
    void shouldOpenCashRegisterSuccessfully() throws Exception {
        OpenCashRequest request = new OpenCashRequest(new BigDecimal("150.00"), "Fabiana Nogueira");

        mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.operator", is("Fabiana Nogueira")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.initialBalance", is(150.00)))
                .andExpect(jsonPath("$.expectedBalance", is(150.00)))
                .andExpect(jsonPath("$.openedAt").isNotEmpty())
                .andExpect(jsonPath("$.closedAt").doesNotExist());
    }

    @Test
    @DisplayName("POST /cash-registers deve retornar 409 quando já houver um caixa aberto")
    void shouldReturn409WhenCashAlreadyOpen() throws Exception {
        OpenCashRequest firstRequest = new OpenCashRequest(new BigDecimal("100.00"), "Operador 1");
        mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        OpenCashRequest secondRequest = new OpenCashRequest(new BigDecimal("200.00"), "Operador 2");
        mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CASH_ALREADY_OPEN")));
    }

    @Test
    @DisplayName("POST /cash-registers/{id}/close deve fechar caixa com sucesso sem diferença")
    void shouldCloseCashRegisterSuccessfullyWithoutDifference() throws Exception {
        OpenCashRequest openReq = new OpenCashRequest(new BigDecimal("150.00"), "Fabiana");
        String responseStr = mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseStr).get("id").asText();

        CloseCashRequest closeReq = new CloseCashRequest(new BigDecimal("150.00"));

        mockMvc.perform(post("/cash-registers/{id}/close", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.countedBalance", is(150.00)))
                .andExpect(jsonPath("$.expectedBalance", is(150.00)))
                .andExpect(jsonPath("$.difference", is(0.00)))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /cash-registers/{id}/close com diferença exige justificativa")
    void shouldRequireJustificationWhenThereIsDifference() throws Exception {
        OpenCashRequest openReq = new OpenCashRequest(new BigDecimal("150.00"), "Fabiana");
        String responseStr = mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseStr).get("id").asText();

        // Sem justificativa -> 400 Bad Request
        CloseCashRequest invalidCloseReq = new CloseCashRequest(new BigDecimal("140.00"), null);
        mockMvc.perform(post("/cash-registers/{id}/close", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCloseReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("JUSTIFICATION_REQUIRED")));

        // Com justificativa -> 200 OK
        CloseCashRequest validCloseReq = new CloseCashRequest(new BigDecimal("140.00"), "Diferença referente a sangria não registrada");
        mockMvc.perform(post("/cash-registers/{id}/close", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCloseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.difference", is(-10.00)))
                .andExpect(jsonPath("$.justification", is("Diferença referente a sangria não registrada")));
    }

    @Test
    @DisplayName("POST /cash-registers/close deve fechar o caixa aberto atual")
    void shouldCloseCurrentOpenCashRegister() throws Exception {
        OpenCashRequest openReq = new OpenCashRequest(new BigDecimal("200.00"), "Operador Geral");
        mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated());

        CloseCashRequest closeReq = new CloseCashRequest(new BigDecimal("200.00"));
        mockMvc.perform(post("/cash-registers/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.countedBalance", is(200.00)));
    }

    @Test
    @DisplayName("GET /cash-registers/current deve retornar o caixa aberto atual")
    void shouldGetCurrentCashRegister() throws Exception {
        // Inicialmente nenhum caixa -> 204 No Content
        mockMvc.perform(get("/cash-registers/current"))
                .andExpect(status().isNoContent());

        // Abre caixa
        OpenCashRequest openReq = new OpenCashRequest(new BigDecimal("120.00"), "Carlos");
        mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated());

        // Agora retorna o caixa aberto
        mockMvc.perform(get("/cash-registers/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operator", is("Carlos")))
                .andExpect(jsonPath("$.initialBalance", is(120.00)))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    @DisplayName("POST /cash-registers/{id}/movements deve registrar movimentação e atualizar saldo esperado")
    void shouldCreateMovementAndUpdateExpectedBalance() throws Exception {
        OpenCashRequest openReq = new OpenCashRequest(new BigDecimal("100.00"), "Fabiana");
        String responseStr = mockMvc.perform(post("/cash-registers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseStr).get("id").asText();

        // Registra entrada por venda
        CreateCashMovementRequest sale = new CreateCashMovementRequest(
                CashMovementDirection.IN, "SALE", new BigDecimal("50.00"), "Venda balcão em dinheiro",
                "PDV", "SALE", UUID.randomUUID());

        mockMvc.perform(post("/cash-registers/{id}/movements", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sale)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction", is("IN")))
                .andExpect(jsonPath("$.type", is("IN")))
                .andExpect(jsonPath("$.amount", is(50.00)));

        // Registra saída por despesa
        CreateCashMovementRequest expense = new CreateCashMovementRequest(
                CashMovementDirection.OUT, "EXPENSE", new BigDecimal("20.00"), "Compra de gelo",
                "Despesa", "EXPENSE", UUID.randomUUID());

        mockMvc.perform(post("/cash-registers/{id}/movements", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction", is("OUT")))
                .andExpect(jsonPath("$.amount", is(20.00)));

        // Verifica resumo financeiro: 100 + 50 - 20 = 130
        mockMvc.perform(get("/cash-registers/{id}/summary", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialBalance", is(100.00)))
                .andExpect(jsonPath("$.cashSales", is(50.00)))
                .andExpect(jsonPath("$.cashExpenses", is(20.00)))
                .andExpect(jsonPath("$.expectedBalance", is(130.00)));

        // Lista movimentações
        mockMvc.perform(get("/cash-registers/{id}/movements", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("POST /cash-registers/{id}/movements deve falhar se o caixa estiver fechado")
    void shouldFailToAddMovementWhenCashIsClosed() throws Exception {
        CashRegister register = new CashRegister(
                UUID.randomUUID(),
                CashRegister.DEFAULT_STORE_ID,
                null,
                "Operador",
                new BigDecimal("100.00"),
                null
        );
        register.setStatus(CashRegisterStatus.CLOSED);
        register.setExpectedBalanceAtClose(new BigDecimal("100.00"));
        register = cashRegisterRepository.save(register);

        CreateCashMovementRequest move = new CreateCashMovementRequest(
                CashMovementDirection.IN, "SALE", new BigDecimal("10.00"), "Tentativa de venda",
                null, null, null);

        mockMvc.perform(post("/cash-registers/{id}/movements", register.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(move)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CASH_CLOSED")));
    }
}
