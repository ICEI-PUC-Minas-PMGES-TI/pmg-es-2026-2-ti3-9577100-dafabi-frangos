package com.dafabi.sales;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.sales.application.SaleService;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.CreateSaleRequest;
import com.dafabi.sales.dto.SaleItemRequest;
import com.dafabi.sales.dto.SaleResponse;
import com.dafabi.sales.mapper.SaleMapper;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
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
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CashRegisterRepository cashRegisterRepository;

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Spy
    private SaleMapper saleMapper = new SaleMapper();

    @InjectMocks
    private SaleService saleService;

    private UUID operatorId;
    private CashRegister openRegister;
    private ProductStock chickenStock;
    private ProductStock farofaStock;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        openRegister = new CashRegister(
                UUID.randomUUID(),
                CashRegister.DEFAULT_STORE_ID,
                operatorId,
                "Fabiana Nogueira",
                new BigDecimal("150.00"),
                OffsetDateTime.now()
        );
        chickenStock = stockOf("Frango assado", "42.90", 5, ProductStatus.ACTIVE);
        farofaStock = stockOf("Farofa", "8.50", 10, ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deve concluir a venda calculando o total e baixando o estoque")
    void shouldCreateSaleSuccessfully() {
        mockOpenRegister();
        mockStock(chickenStock);
        mockStock(farofaStock);
        when(saleRepository.findMaxSaleNumber()).thenReturn(7L);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chickenStock.getProductId(), 2),
                new SaleItemRequest(farofaStock.getProductId(), 3)
        ), PaymentMethod.PIX);

        SaleResponse response = saleService.create(request);

        assertThat(response.status()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(response.origin()).isEqualTo(SaleOrigin.COUNTER);
        assertThat(response.operatorId()).isEqualTo(operatorId);
        assertThat(response.cashRegisterId()).isEqualTo(openRegister.getId());
        assertThat(response.saleNumber()).isEqualTo(8L);
        assertThat(response.dateTime()).isNotNull();
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("111.30"));
        assertThat(response.items()).hasSize(2);

        assertThat(chickenStock.getQuantity()).isEqualTo(3);
        assertThat(farofaStock.getQuantity()).isEqualTo(7);

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(movementCaptor.capture());
        assertThat(movementCaptor.getAllValues())
                .allMatch(movement -> movement.getMovementType() == StockMovementType.COUNTER_SALE);

        verify(cashRegisterService, never()).recordMovement(any(), any(), anyString(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve somar itens repetidos do mesmo produto no carrinho")
    void shouldMergeRepeatedItems() {
        mockOpenRegister();
        mockStock(farofaStock);
        when(saleRepository.findMaxSaleNumber()).thenReturn(0L);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(farofaStock.getProductId(), 1),
                new SaleItemRequest(farofaStock.getProductId(), 2)
        ), PaymentMethod.DEBIT);

        SaleResponse response = saleService.create(request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(farofaStock.getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Deve registrar entrada no caixa quando o pagamento for em dinheiro")
    void shouldRecordCashMovementWhenPaymentIsCash() {
        mockOpenRegister();
        mockStock(chickenStock);
        when(saleRepository.findMaxSaleNumber()).thenReturn(0L);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chickenStock.getProductId(), 1)
        ), PaymentMethod.CASH);

        SaleResponse response = saleService.create(request);

        verify(cashRegisterService).recordMovement(
                eq(openRegister.getId()),
                eq(CashMovementDirection.IN),
                eq("SALE"),
                eq(response.total()),
                anyString(),
                anyString(),
                eq("SALE"),
                eq(response.id()));
    }

    @Test
    @DisplayName("Deve falhar quando não houver caixa aberto")
    void shouldFailWhenCashRegisterIsClosed() {
        when(cashRegisterRepository.findByStoreIdAndStatusForUpdate(CashRegister.DEFAULT_STORE_ID, CashRegisterStatus.OPEN))
                .thenReturn(Optional.empty());

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chickenStock.getProductId(), 1)
        ), PaymentMethod.CASH);

        assertThatThrownBy(() -> saleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não existe caixa aberto");

        verify(productStockRepository, never()).findByProductIdForUpdate(any());
        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar quando o operador informado não for o dono do caixa aberto")
    void shouldFailWhenOperatorDoesNotOwnOpenRegister() {
        mockOpenRegister();

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chickenStock.getProductId(), 1)
        ), PaymentMethod.PIX, null, UUID.randomUUID());

        assertThatThrownBy(() -> saleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não existe caixa aberto para o operador informado");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir a venda quando a quantidade for maior que o estoque")
    void shouldFailWhenStockIsInsufficient() {
        mockOpenRegister();
        mockStock(chickenStock);
        when(saleRepository.findMaxSaleNumber()).thenReturn(0L);

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(chickenStock.getProductId(), 6)
        ), PaymentMethod.PIX);

        assertThatThrownBy(() -> saleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estoque insuficiente");

        assertThat(chickenStock.getQuantity()).isEqualTo(5);
        verify(stockMovementRepository, never()).save(any());
        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir a venda de produto inativo")
    void shouldFailWhenProductIsInactive() {
        ProductStock inactiveStock = stockOf("Produto inativo", "10.00", 5, ProductStatus.INACTIVE);
        mockOpenRegister();
        mockStock(inactiveStock);
        when(saleRepository.findMaxSaleNumber()).thenReturn(0L);

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(inactiveStock.getProductId(), 1)
        ), PaymentMethod.PIX);

        assertThatThrownBy(() -> saleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("está inativo");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o produto não existir")
    void shouldFailWhenProductNotFound() {
        UUID missingProductId = UUID.randomUUID();
        mockOpenRegister();
        when(saleRepository.findMaxSaleNumber()).thenReturn(0L);
        when(productStockRepository.findByProductIdForUpdate(missingProductId)).thenReturn(Optional.empty());

        CreateSaleRequest request = new CreateSaleRequest(List.of(
                new SaleItemRequest(missingProductId, 1)
        ), PaymentMethod.PIX);

        assertThatThrownBy(() -> saleService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar venda inexistente")
    void shouldFailWhenSaleNotFound() {
        UUID saleId = UUID.randomUUID();
        when(saleRepository.findWithItemsById(saleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.findById(saleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venda não encontrado");
    }

    private void mockOpenRegister() {
        when(cashRegisterRepository.findByStoreIdAndStatusForUpdate(CashRegister.DEFAULT_STORE_ID, CashRegisterStatus.OPEN))
                .thenReturn(Optional.of(openRegister));
    }

    private void mockStock(ProductStock stock) {
        when(productStockRepository.findByProductIdForUpdate(stock.getProductId())).thenReturn(Optional.of(stock));
    }

    private ProductStock stockOf(String name, String price, int quantity, ProductStatus status) {
        Product product = new Product(UUID.randomUUID(), null, name,
                new BigDecimal(price), new BigDecimal("1.00"), "UN", null,
                false, true, status, null, null);
        ProductStock stock = new ProductStock(product, quantity);
        product.setStock(stock);
        return stock;
    }
}
