package com.dafabi.cash;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovement;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.dto.CloseCashRequest;
import com.dafabi.cash.dto.CreateCashMovementRequest;
import com.dafabi.cash.dto.CashMovementResponse;
import com.dafabi.cash.dto.CashRegisterResponse;
import com.dafabi.cash.dto.CashRegisterSummaryResponse;
import com.dafabi.cash.dto.OpenCashRequest;
import com.dafabi.cash.mapper.CashRegisterMapper;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashRegisterServiceTest {

    @Mock
    private CashRegisterRepository cashRegisterRepository;

    @Mock
    private CashMovementRepository cashMovementRepository;

    @Spy
    private CashRegisterMapper cashRegisterMapper = new CashRegisterMapper();

    @InjectMocks
    private CashRegisterService cashRegisterService;

    private UUID storeId;
    private CashRegister openRegister;

    @BeforeEach
    void setUp() {
        storeId = CashRegister.DEFAULT_STORE_ID;
        openRegister = new CashRegister(
                UUID.randomUUID(),
                storeId,
                UUID.randomUUID(),
                "Fabiana Nogueira",
                new BigDecimal("150.00"),
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Deve abrir o caixa com sucesso quando não há caixa aberto")
    void shouldOpenCashRegisterSuccessfully() {
        OpenCashRequest request = new OpenCashRequest(new BigDecimal("150.00"), "Fabiana Nogueira");

        when(cashRegisterRepository.existsByStoreIdAndStatus(storeId, CashRegisterStatus.OPEN)).thenReturn(false);
        when(cashRegisterRepository.save(any(CashRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashRegisterResponse response = cashRegisterService.openCashRegister(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(CashRegisterStatus.OPEN);
        assertThat(response.operator()).isEqualTo("Fabiana Nogueira");
        assertThat(response.initialBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.expectedBalance()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(cashRegisterRepository).save(any(CashRegister.class));
    }

    @Test
    @DisplayName("Deve falhar ao abrir caixa se já houver um caixa aberto na loja")
    void shouldFailWhenCashAlreadyOpen() {
        OpenCashRequest request = new OpenCashRequest(new BigDecimal("100.00"));

        when(cashRegisterRepository.existsByStoreIdAndStatus(storeId, CashRegisterStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> cashRegisterService.openCashRegister(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um caixa aberto");

        verify(cashRegisterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve fechar caixa com sucesso quando saldo contado bate com saldo esperado")
    void shouldCloseCashRegisterSuccessfullyWithoutDifference() {
        UUID registerId = openRegister.getId();
        CloseCashRequest request = new CloseCashRequest(new BigDecimal("150.00"));

        when(cashRegisterRepository.findById(registerId)).thenReturn(Optional.of(openRegister));
        when(cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(registerId)).thenReturn(List.of());
        when(cashRegisterRepository.save(any(CashRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashRegisterResponse response = cashRegisterService.closeCashRegister(registerId, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(CashRegisterStatus.CLOSED);
        assertThat(response.countedBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.expectedBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.difference()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.closedAt()).isNotNull();

        verify(cashRegisterRepository).save(openRegister);
    }

    @Test
    @DisplayName("Deve fechar caixa com diferença quando justificativa válida for fornecida")
    void shouldCloseCashRegisterWithDifferenceAndJustification() {
        UUID registerId = openRegister.getId();
        CloseCashRequest request = new CloseCashRequest(new BigDecimal("140.00"), "Falta troco no caixa");

        when(cashRegisterRepository.findById(registerId)).thenReturn(Optional.of(openRegister));
        when(cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(registerId)).thenReturn(List.of());
        when(cashRegisterRepository.save(any(CashRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashRegisterResponse response = cashRegisterService.closeCashRegister(registerId, request);

        assertThat(response.status()).isEqualTo(CashRegisterStatus.CLOSED);
        assertThat(response.difference()).isEqualByComparingTo(new BigDecimal("-10.00"));
        assertThat(response.justification()).isEqualTo("Falta troco no caixa");
    }

    @Test
    @DisplayName("Deve falhar ao fechar caixa com diferença sem justificativa")
    void shouldFailWhenClosingWithDifferenceWithoutJustification() {
        UUID registerId = openRegister.getId();
        CloseCashRequest request = new CloseCashRequest(new BigDecimal("140.00"), null);

        when(cashRegisterRepository.findById(registerId)).thenReturn(Optional.of(openRegister));
        when(cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(registerId)).thenReturn(List.of());

        assertThatThrownBy(() -> cashRegisterService.closeCashRegister(registerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("justificativa é obrigatória");

        verify(cashRegisterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao tentar fechar um caixa que já se encontra fechado")
    void shouldFailWhenClosingAlreadyClosedCash() {
        UUID registerId = openRegister.getId();
        openRegister.setStatus(CashRegisterStatus.CLOSED);
        CloseCashRequest request = new CloseCashRequest(new BigDecimal("150.00"));

        when(cashRegisterRepository.findById(registerId)).thenReturn(Optional.of(openRegister));

        assertThatThrownBy(() -> cashRegisterService.closeCashRegister(registerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já se encontra fechado");

        verify(cashRegisterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve calcular saldo esperado considerando movimentações de venda, despesa e estorno")
    void shouldCalculateExpectedBalanceWithMovements() {
        UUID registerId = openRegister.getId();
        CashMovement sale = new CashMovement(openRegister, CashMovementDirection.IN, "SALE",
                new BigDecimal("50.00"), "PDV", "Venda em dinheiro", "SALE", UUID.randomUUID());
        CashMovement expense = new CashMovement(openRegister, CashMovementDirection.OUT, "EXPENSE",
                new BigDecimal("20.00"), "Despesa", "Gelo", "EXPENSE", UUID.randomUUID());
        CashMovement refund = new CashMovement(openRegister, CashMovementDirection.OUT, "REFUND",
                new BigDecimal("10.00"), "Estorno", "Devolução", "REFUND", UUID.randomUUID());

        when(cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(registerId))
                .thenReturn(List.of(sale, expense, refund));

        BigDecimal expected = cashRegisterService.calculateExpectedBalance(openRegister);
        // 150 + 50 - 20 - 10 = 170
        assertThat(expected).isEqualByComparingTo(new BigDecimal("170.00"));
    }

    @Test
    @DisplayName("Deve falhar ao registrar movimentação em caixa fechado")
    void shouldFailToRegisterMovementInClosedCash() {
        UUID registerId = openRegister.getId();
        openRegister.setStatus(CashRegisterStatus.CLOSED);

        CreateCashMovementRequest request = new CreateCashMovementRequest(
                CashMovementDirection.IN, "SALE", new BigDecimal("50.00"), "Venda", null, null, null);

        when(cashRegisterRepository.findById(registerId)).thenReturn(Optional.of(openRegister));

        assertThatThrownBy(() -> cashRegisterService.createMovement(registerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível registrar movimentação em um caixa fechado");
    }

    @Test
    @DisplayName("Deve gerar resumo financeiro do caixa com totais discriminados")
    void shouldGenerateSummaryCorrectly() {
        UUID registerId = openRegister.getId();
        CashMovement sale = new CashMovement(openRegister, CashMovementDirection.IN, "SALE",
                new BigDecimal("42.90"), "PDV", "Venda balcão", "SALE", UUID.randomUUID());
        CashMovement expense = new CashMovement(openRegister, CashMovementDirection.OUT, "EXPENSE",
                new BigDecimal("15.00"), "Despesa", "Carvão", "EXPENSE", UUID.randomUUID());

        when(cashRegisterRepository.findById(registerId)).thenReturn(Optional.of(openRegister));
        when(cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(registerId))
                .thenReturn(List.of(sale, expense));

        CashRegisterSummaryResponse summary = cashRegisterService.getSummary(registerId);

        assertThat(summary).isNotNull();
        assertThat(summary.initialBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(summary.cashSales()).isEqualByComparingTo(new BigDecimal("42.90"));
        assertThat(summary.cashExpenses()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(summary.expectedBalance()).isEqualByComparingTo(new BigDecimal("177.90"));
    }
}
