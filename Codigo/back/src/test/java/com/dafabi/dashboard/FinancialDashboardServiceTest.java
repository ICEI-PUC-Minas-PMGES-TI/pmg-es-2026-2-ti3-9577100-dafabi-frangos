package com.dafabi.dashboard;

import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.dashboard.application.FinancialDashboardService;
import com.dafabi.dashboard.dto.FinancialDashboardResponse;
import com.dafabi.expenses.repository.ExpenseRepository;
import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceOrder;
import com.dafabi.integrations.repository.MarketplaceOrderRepository;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialDashboardServiceTest {
    @Mock private SaleRepository saleRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private CashMovementRepository cashMovementRepository;
    @Mock private MarketplaceOrderRepository marketplaceOrderRepository;

    private FinancialDashboardService service;

    @BeforeEach
    void setUp() {
        service = new FinancialDashboardService(saleRepository, expenseRepository, cashMovementRepository, marketplaceOrderRepository);
    }

    private void stubNoData() {
        when(saleRepository.findByDateTimeGreaterThanEqualAndDateTimeLessThan(any(), any())).thenReturn(List.of());
        when(expenseRepository.findByDateGreaterThanEqualAndDateLessThanEqual(any(), any())).thenReturn(List.of());
        when(cashMovementRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any()))
                .thenReturn(List.of());
        when(marketplaceOrderRepository.findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThan(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void shouldReturnZeroedDailySeriesWhenPeriodHasNoData() {
        stubNoData();
        FinancialDashboardResponse response = service.getSummary(
                "custom", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

        assertThat(response.summary().revenue()).isZero();
        assertThat(response.summary().salesCount()).isZero();
        assertThat(response.daily()).extracting(FinancialDashboardResponse.DailyRevenue::revenue)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"));
    }

    @Test
    void shouldCalculateRevenueAndOriginCountsFromCompletedSales() {
        when(expenseRepository.findByDateGreaterThanEqualAndDateLessThanEqual(any(), any())).thenReturn(List.of());
        when(cashMovementRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any()))
                .thenReturn(List.of());
        Sale sale = new Sale(UUID.randomUUID(), null, null, 1L,
                OffsetDateTime.parse("2026-10-02T12:00:00-03:00"),
                SaleOrigin.IFOOD, PaymentMethod.PIX, new BigDecimal("30.00"),
                new BigDecimal("30.00"), SaleStatus.COMPLETED);
        Sale cancelled = new Sale(UUID.randomUUID(), null, null, 2L,
                OffsetDateTime.parse("2026-10-02T13:00:00-03:00"),
                SaleOrigin.COUNTER, PaymentMethod.CASH, new BigDecimal("99.00"),
                new BigDecimal("99.00"), SaleStatus.CANCELLED);
        when(saleRepository.findByDateTimeGreaterThanEqualAndDateTimeLessThan(any(), any()))
                .thenReturn(List.of(sale, cancelled));

        FinancialDashboardResponse response = service.getSummary(
                "custom", LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 2));

        assertThat(response.summary().revenue()).isEqualByComparingTo("30.00");
        assertThat(response.summary().salesCount()).isEqualTo(1);
        assertThat(response.salesByOrigin()).containsEntry("IFOOD", 1L).containsEntry("COUNTER", 0L);
        assertThat(response.daily().get(0).revenue()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldTreatMarketplaceFeesAsCostsRegardlessOfTheirSign() {
        MarketplaceOrder ifoodNegative = marketplaceOrder(IntegrationProvider.IFOOD, "-4.00", "-1.00");
        MarketplaceOrder ifoodPositive = marketplaceOrder(IntegrationProvider.IFOOD, "4.00", "1.00");
        MarketplaceOrder food99Negative = marketplaceOrder(IntegrationProvider.FOOD_99, "-7.00", null);
        MarketplaceOrder food99Positive = marketplaceOrder(IntegrationProvider.FOOD_99, "7.00", null);
        when(saleRepository.findByDateTimeGreaterThanEqualAndDateTimeLessThan(any(), any())).thenReturn(List.of());
        when(expenseRepository.findByDateGreaterThanEqualAndDateLessThanEqual(any(), any())).thenReturn(List.of());
        when(cashMovementRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any()))
                .thenReturn(List.of());
        when(marketplaceOrderRepository.findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThan(
                eq(IntegrationProvider.IFOOD), any(), any())).thenReturn(List.of(ifoodNegative, ifoodPositive));
        when(marketplaceOrderRepository.findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThan(
                eq(IntegrationProvider.FOOD_99), any(), any())).thenReturn(List.of(food99Negative, food99Positive));

        FinancialDashboardResponse response = service.getSummary(
                "custom", LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 2));

        assertThat(response.summary().ifoodFees()).isEqualByComparingTo("10.00");
        assertThat(response.summary().food99Fees()).isEqualByComparingTo("14.00");
        assertThat(response.summary().fees()).isEqualByComparingTo("24.00");
        assertThat(response.summary().estimatedResult()).isEqualByComparingTo("376.00");
    }

    private MarketplaceOrder marketplaceOrder(IntegrationProvider provider, String commission, String serviceFee) {
        MarketplaceOrder order = mock(MarketplaceOrder.class);
        when(order.getId()).thenReturn(UUID.randomUUID());
        when(order.getProvider()).thenReturn(provider);
        when(order.getNormalizedStatus()).thenReturn("COMPLETED");
        when(order.getOrderAt()).thenReturn(OffsetDateTime.parse("2026-10-02T12:00:00-03:00"));
        when(order.getItemsValue()).thenReturn(new BigDecimal("100.00"));
        when(order.getFeesAndCommissions()).thenReturn(new BigDecimal(commission));
        if (serviceFee != null) {
            when(order.getServiceFee()).thenReturn(new BigDecimal(serviceFee));
        }
        return order;
    }

    @Test
    void shouldRejectInvalidCustomPeriod() {
        assertThatThrownBy(() -> service.getSummary("custom", LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Período personalizado");
    }
}
