package com.dafabi.sales;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovement;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.sales.application.SaleRefundOutcome;
import com.dafabi.sales.application.SaleRefundService;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleItem;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleRefund;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.CreateSaleRefundRequest;
import com.dafabi.sales.dto.SaleRefundEligibilityResponse;
import com.dafabi.sales.mapper.SaleMapper;
import com.dafabi.sales.mapper.SaleRefundMapper;
import com.dafabi.sales.repository.SaleRefundRepository;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.shared.security.RequestUser;
import com.dafabi.shared.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleRefundServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleRefundRepository saleRefundRepository;

    @Mock
    private CashRegisterRepository cashRegisterRepository;

    @Mock
    private CashMovementRepository cashMovementRepository;

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Spy
    private SaleRefundMapper saleRefundMapper = new SaleRefundMapper(new SaleMapper());

    @InjectMocks
    private SaleRefundService saleRefundService;

    private final RequestUser admin = new RequestUser(UUID.randomUUID(), "Fabiana Nogueira", UserRole.ADMIN);
    private final RequestUser cashier = new RequestUser(UUID.randomUUID(), "Carlos Oliveira", UserRole.CASHIER);
    private final CreateSaleRefundRequest request = new CreateSaleRefundRequest("  Cliente desistiu da compra  ");

    private CashRegister openRegister;
    private ProductStock chickenStock;
    private ProductStock farofaStock;

    @BeforeEach
    void setUp() {
        openRegister = new CashRegister(
                UUID.randomUUID(),
                CashRegister.DEFAULT_STORE_ID,
                UUID.randomUUID(),
                "Fabiana Nogueira",
                new BigDecimal("100.00"),
                OffsetDateTime.now()
        );
        chickenStock = stockOf("Frango assado", "42.90", 3);
        farofaStock = stockOf("Farofa", "8.50", 7);
    }

    @Test
    @DisplayName("Deve estornar venda em dinheiro devolvendo o estoque e retirando o valor do caixa")
    void shouldRefundCashSaleReversingStockAndCash() {
        Sale sale = saleOf(PaymentMethod.CASH, SaleStatus.COMPLETED);
        mockLockedSale(sale);
        mockCashIn(sale, "102.80");
        when(cashRegisterRepository.findById(openRegister.getId())).thenReturn(Optional.of(openRegister));
        when(cashRegisterRepository.findByStoreIdAndStatusForUpdate(CashRegister.DEFAULT_STORE_ID, CashRegisterStatus.OPEN))
                .thenReturn(Optional.of(openRegister));
        mockStock(chickenStock);
        mockStock(farofaStock);
        when(saleRefundRepository.save(any(SaleRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleRefundOutcome outcome = saleRefundService.refund(sale.getId(), request, admin, null);

        assertThat(outcome.created()).isTrue();
        assertThat(outcome.refund().saleStatus()).isEqualTo(SaleStatus.REFUNDED);
        assertThat(outcome.refund().amount()).isEqualByComparingTo("102.80");
        assertThat(outcome.refund().cashAmount()).isEqualByComparingTo("102.80");
        assertThat(outcome.refund().reason()).isEqualTo("Cliente desistiu da compra");
        assertThat(outcome.refund().refundedById()).isEqualTo(admin.id());
        assertThat(outcome.refund().refundedBy()).isEqualTo("Fabiana Nogueira");
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.REFUNDED);

        assertThat(chickenStock.getQuantity()).isEqualTo(5);
        assertThat(farofaStock.getQuantity()).isEqualTo(9);

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(movementCaptor.capture());
        assertThat(movementCaptor.getAllValues())
                .allMatch(movement -> movement.getMovementType() == StockMovementType.REFUND
                        && movement.getQuantityDelta() == 2
                        && movement.getPerformedBy().equals("Fabiana Nogueira"));

        verify(cashRegisterService).recordMovement(
                eq(openRegister.getId()),
                eq(CashMovementDirection.OUT),
                eq("REFUND"),
                argThat(amount -> amount.compareTo(new BigDecimal("102.80")) == 0),
                anyString(),
                anyString(),
                eq("SALE_REFUND"),
                eq(outcome.refund().id()));
    }

    @Test
    @DisplayName("Deve estornar venda por PIX sem movimentar o caixa")
    void shouldRefundPixSaleWithoutCashMovement() {
        Sale sale = saleOf(PaymentMethod.PIX, SaleStatus.COMPLETED);
        mockLockedSale(sale);
        when(cashMovementRepository.findByReferenceTypeAndReferenceId("SALE", sale.getId())).thenReturn(List.of());
        mockStock(chickenStock);
        mockStock(farofaStock);
        when(saleRefundRepository.save(any(SaleRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleRefundOutcome outcome = saleRefundService.refund(sale.getId(), request, admin, null);

        assertThat(outcome.refund().cashAmount()).isEqualByComparingTo("0.00");
        assertThat(outcome.refund().cashRegisterId()).isNull();
        assertThat(chickenStock.getQuantity()).isEqualTo(5);
        verify(cashRegisterRepository, never()).findByStoreIdAndStatusForUpdate(any(), any());
        verifyNoInteractions(cashRegisterService);
    }

    @Test
    @DisplayName("Deve limitar o nome do responsável ao tamanho aceito pelas movimentações de estoque")
    void shouldTruncateLongResponsibleName() {
        RequestUser longNameAdmin = new RequestUser(UUID.randomUUID(), "A".repeat(200), UserRole.ADMIN);
        Sale sale = saleOf(PaymentMethod.PIX, SaleStatus.COMPLETED);
        mockLockedSale(sale);
        when(cashMovementRepository.findByReferenceTypeAndReferenceId("SALE", sale.getId())).thenReturn(List.of());
        mockStock(chickenStock);
        mockStock(farofaStock);
        when(saleRefundRepository.save(any(SaleRefund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleRefundOutcome outcome = saleRefundService.refund(sale.getId(), request, longNameAdmin, null);

        assertThat(outcome.refund().refundedBy()).hasSize(100);
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(movementCaptor.capture());
        assertThat(movementCaptor.getAllValues()).allMatch(movement -> movement.getPerformedBy().length() == 100);
    }

    @Test
    @DisplayName("Deve impedir que um usuário sem perfil ADMIN estorne a venda")
    void shouldRejectRefundFromNonAdmin() {
        assertThatThrownBy(() -> saleRefundService.refund(UUID.randomUUID(), request, cashier, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ACCESS_DENIED");

        verifyNoInteractions(saleRepository, saleRefundRepository, productStockRepository,
                stockMovementRepository, cashRegisterService);
    }

    @Test
    @DisplayName("Deve bloquear o estorno de venda cancelada")
    void shouldRejectCancelledSale() {
        Sale sale = saleOf(PaymentMethod.CASH, SaleStatus.CANCELLED);
        mockLockedSale(sale);

        assertThatThrownBy(() -> saleRefundService.refund(sale.getId(), request, admin, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SALE_CANCELLED");

        assertNoReversal();
    }

    @Test
    @DisplayName("Deve bloquear um segundo estorno da mesma venda sem duplicar reversões")
    void shouldRejectAlreadyRefundedSale() {
        Sale sale = saleOf(PaymentMethod.CASH, SaleStatus.REFUNDED);
        mockLockedSale(sale);
        when(saleRefundRepository.findBySale_Id(sale.getId()))
                .thenReturn(Optional.of(refundOf(sale, "chave-original")));

        assertThatThrownBy(() -> saleRefundService.refund(sale.getId(), request, admin, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SALE_ALREADY_REFUNDED");

        assertNoReversal();
    }

    @Test
    @DisplayName("Deve devolver o estorno existente quando a mesma chave de idempotência for reenviada")
    void shouldReturnExistingRefundForSameIdempotencyKey() {
        Sale sale = saleOf(PaymentMethod.CASH, SaleStatus.REFUNDED);
        SaleRefund existing = refundOf(sale, "chave-1");
        mockLockedSale(sale);
        when(saleRefundRepository.findByIdempotencyKey("chave-1")).thenReturn(Optional.of(existing));

        SaleRefundOutcome outcome = saleRefundService.refund(sale.getId(), request, admin, " chave-1 ");

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.refund().id()).isEqualTo(existing.getId());
        assertNoReversal();
    }

    @Test
    @DisplayName("Deve rejeitar chave de idempotência já usada em outra venda")
    void shouldRejectIdempotencyKeyFromAnotherSale() {
        Sale sale = saleOf(PaymentMethod.CASH, SaleStatus.COMPLETED);
        Sale otherSale = saleOf(PaymentMethod.CASH, SaleStatus.REFUNDED);
        mockLockedSale(sale);
        when(saleRefundRepository.findByIdempotencyKey("chave-1"))
                .thenReturn(Optional.of(refundOf(otherSale, "chave-1")));

        assertThatThrownBy(() -> saleRefundService.refund(sale.getId(), request, admin, "chave-1"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "IDEMPOTENCY_KEY_REUSED");

        assertNoReversal();
    }

    @Test
    @DisplayName("Deve exigir caixa aberto para devolver o valor de venda em dinheiro")
    void shouldRequireOpenCashRegisterForCashRefund() {
        Sale sale = saleOf(PaymentMethod.CASH, SaleStatus.COMPLETED);
        mockLockedSale(sale);
        mockCashIn(sale, "102.80");
        when(cashRegisterRepository.findById(openRegister.getId())).thenReturn(Optional.of(openRegister));
        when(cashRegisterRepository.findByStoreIdAndStatusForUpdate(CashRegister.DEFAULT_STORE_ID, CashRegisterStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleRefundService.refund(sale.getId(), request, admin, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "CASH_CLOSED");

        assertNoReversal();
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.COMPLETED);
    }

    @Test
    @DisplayName("Deve lançar exceção quando a venda não existir")
    void shouldThrowWhenSaleDoesNotExist() {
        UUID saleId = UUID.randomUUID();
        when(saleRepository.findByIdForUpdate(saleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleRefundService.refund(saleId, request, admin, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve informar se a venda é elegível ao estorno")
    void shouldReportEligibility() {
        Sale completed = saleOf(PaymentMethod.CASH, SaleStatus.COMPLETED);
        when(saleRepository.findWithItemsById(completed.getId())).thenReturn(Optional.of(completed));
        mockCashIn(completed, "102.80");

        SaleRefundEligibilityResponse eligible = saleRefundService.findEligibility(completed.getId());

        assertThat(eligible.eligible()).isTrue();
        assertThat(eligible.ineligibilityCode()).isNull();
        assertThat(eligible.cashAmountToReverse()).isEqualByComparingTo("102.80");
        assertThat(eligible.items()).hasSize(2);

        Sale cancelled = saleOf(PaymentMethod.CASH, SaleStatus.CANCELLED);
        when(saleRepository.findWithItemsById(cancelled.getId())).thenReturn(Optional.of(cancelled));

        SaleRefundEligibilityResponse ineligible = saleRefundService.findEligibility(cancelled.getId());

        assertThat(ineligible.eligible()).isFalse();
        assertThat(ineligible.ineligibilityCode()).isEqualTo("SALE_CANCELLED");
    }

    private void assertNoReversal() {
        verify(saleRefundRepository, never()).save(any());
        verify(productStockRepository, never()).findByProductIdForUpdate(any());
        verify(stockMovementRepository, never()).save(any());
        verifyNoInteractions(cashRegisterService);
    }

    private void mockLockedSale(Sale sale) {
        when(saleRepository.findByIdForUpdate(sale.getId())).thenReturn(Optional.of(sale));
    }

    private void mockCashIn(Sale sale, String amount) {
        CashMovement movement = new CashMovement(openRegister, CashMovementDirection.IN, "SALE",
                new BigDecimal(amount), "Venda balcão", "Venda nº " + sale.getSaleNumber(), "SALE", sale.getId());
        when(cashMovementRepository.findByReferenceTypeAndReferenceId("SALE", sale.getId()))
                .thenReturn(List.of(movement));
    }

    private void mockStock(ProductStock stock) {
        when(productStockRepository.findByProductIdForUpdate(stock.getProductId())).thenReturn(Optional.of(stock));
    }

    private Sale saleOf(PaymentMethod paymentMethod, SaleStatus status) {
        Sale sale = new Sale(UUID.randomUUID(), openRegister.getId(), openRegister.getOperatorId(), 12L,
                OffsetDateTime.now(), SaleOrigin.COUNTER, paymentMethod,
                new BigDecimal("102.80"), new BigDecimal("102.80"), status);
        sale.addItem(new SaleItem(UUID.randomUUID(), sale, chickenStock.getProduct(),
                new BigDecimal("2.000"), new BigDecimal("42.90"), new BigDecimal("85.80")));
        sale.addItem(new SaleItem(UUID.randomUUID(), sale, farofaStock.getProduct(),
                new BigDecimal("2.000"), new BigDecimal("8.50"), new BigDecimal("17.00")));
        return sale;
    }

    private SaleRefund refundOf(Sale sale, String idempotencyKey) {
        return new SaleRefund(UUID.randomUUID(), sale, sale.getTotal(), sale.getTotal(), openRegister.getId(),
                "Estorno anterior", admin.id(), admin.name(), idempotencyKey);
    }

    private ProductStock stockOf(String name, String price, int quantity) {
        Product product = new Product(UUID.randomUUID(), null, name,
                new BigDecimal(price), new BigDecimal("1.00"), "UN", null,
                false, true, ProductStatus.ACTIVE, null, null);
        ProductStock stock = new ProductStock(product, quantity);
        product.setStock(stock);
        return stock;
    }
}
