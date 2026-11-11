package com.dafabi.dashboard.application;

import com.dafabi.cash.domain.CashMovement;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.repository.CashMovementRepository;
import com.dafabi.dashboard.dto.FinancialDashboardResponse;
import com.dafabi.expenses.domain.Expense;
import com.dafabi.expenses.domain.ExpenseStatus;
import com.dafabi.expenses.repository.ExpenseRepository;
import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceOrder;
import com.dafabi.integrations.repository.MarketplaceOrderRepository;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialDashboardService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;
    private final CashMovementRepository cashMovementRepository;
    private final MarketplaceOrderRepository marketplaceOrderRepository;

    public FinancialDashboardService(SaleRepository saleRepository,
                                     ExpenseRepository expenseRepository,
                                     CashMovementRepository cashMovementRepository,
                                     MarketplaceOrderRepository marketplaceOrderRepository) {
        this.saleRepository = saleRepository;
        this.expenseRepository = expenseRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.marketplaceOrderRepository = marketplaceOrderRepository;
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getSummary(String period, LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(period, startDate, endDate);
        OffsetDateTime start = range.start().atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime endExclusive = range.end().plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();

        List<Sale> sales = saleRepository.findByDateTimeGreaterThanEqualAndDateTimeLessThan(start, endExclusive)
                .stream()
                .filter(sale -> sale.getStatus() == SaleStatus.COMPLETED)
                .toList();
        List<Expense> expenses = expenseRepository.findByDateGreaterThanEqualAndDateLessThanEqual(
                        range.start(), range.end()).stream()
                .filter(expense -> expense.getStatus() != ExpenseStatus.CANCELLED)
                .toList();
        List<MarketplaceOrder> marketplaceOrders = java.util.Arrays.stream(IntegrationProvider.values())
                .flatMap(provider -> marketplaceOrderRepository
                        .findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThan(provider, start, endExclusive).stream())
                .filter(order -> "COMPLETED".equals(order.getNormalizedStatus()))
                .toList();
        List<CashMovement> movements = cashMovementRepository
                .findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(start, endExclusive);

        Map<SaleOrigin, Long> originCounts = new EnumMap<>(SaleOrigin.class);
        for (SaleOrigin origin : SaleOrigin.values()) {
            originCounts.put(origin, sales.stream().filter(sale -> sale.getOrigin() == origin).count());
        }

        BigDecimal revenue = sum(sales.stream().map(Sale::getTotal).toList())
                .add(sum(marketplaceOrders.stream().map(MarketplaceOrder::getItemsValue).toList()));
        BigDecimal expenseTotal = sum(expenses.stream().map(Expense::getAmount).toList());
        BigDecimal ifoodFees = sum(marketplaceOrders.stream().filter(o -> o.getProvider() == IntegrationProvider.IFOOD)
                .map(o -> zero(o.getFeesAndCommissions()).add(zero(o.getServiceFee()))).toList());
        BigDecimal food99Fees = sum(marketplaceOrders.stream().filter(o -> o.getProvider() == IntegrationProvider.FOOD_99)
                .map(o -> zero(o.getFeesAndCommissions())).toList());
        BigDecimal otherMarketplaceCosts = sum(marketplaceOrders.stream().map(o -> zero(o.getMarketingCost())
                .add(zero(o.getLogisticsCost())).add(zero(o.getRefundAmount())).add(zero(o.getFreeDeliveryCost()))
                .add(o.getProvider() == IntegrationProvider.IFOOD ? zero(o.getStorePromotion()) : BigDecimal.ZERO)).toList());
        boolean marketplaceFeesComplete = marketplaceOrders.stream().allMatch(o -> o.getFeesAndCommissions() != null
                && (o.getProvider() == IntegrationProvider.IFOOD
                    ? o.getServiceFee() != null && o.getStorePromotion() != null
                    : o.getMarketingCost() != null && o.getFreeDeliveryCost() != null
                        && o.getLogisticsCost() != null && o.getRefundAmount() != null));
        BigDecimal fees = ifoodFees.add(food99Fees);
        BigDecimal estimatedResult = revenue.subtract(expenseTotal).subtract(fees).subtract(otherMarketplaceCosts)
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Long> salesByOrigin = new LinkedHashMap<>();
        salesByOrigin.put("COUNTER", originCounts.get(SaleOrigin.COUNTER));
        salesByOrigin.put("IFOOD", originCounts.get(SaleOrigin.IFOOD) + marketplaceOrders.stream()
                .filter(o -> o.getProvider() == IntegrationProvider.IFOOD).count());
        salesByOrigin.put("FOOD_99", originCounts.get(SaleOrigin.FOOD_99) + marketplaceOrders.stream()
                .filter(o -> o.getProvider() == IntegrationProvider.FOOD_99).count());

        return new FinancialDashboardResponse(
                range.start(),
                range.end(),
                new FinancialDashboardResponse.Summary(
                        revenue,
                        expenseTotal,
                        fees,
                        ifoodFees,
                        food99Fees,
                        otherMarketplaceCosts,
                        marketplaceFeesComplete,
                        estimatedResult,
                        sales.size() + marketplaceOrders.size(),
                        originCounts.get(SaleOrigin.COUNTER),
                        salesByOrigin.get("IFOOD"),
                        salesByOrigin.get("FOOD_99"),
                        sum(movements.stream().filter(m -> m.getDirection() == CashMovementDirection.IN)
                                .map(CashMovement::getAmount).toList()),
                        sum(movements.stream().filter(m -> m.getDirection() == CashMovementDirection.OUT)
                                .map(CashMovement::getAmount).toList())),
                salesByOrigin,
                buildDailyRevenue(sales, marketplaceOrders, range.start(), range.end()),
                buildRecentSales(sales, marketplaceOrders));
    }

    private List<FinancialDashboardResponse.DailyRevenue> buildDailyRevenue(
            List<Sale> sales, List<MarketplaceOrder> marketplaceOrders, LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            totals.put(date, ZERO);
        }
        for (Sale sale : sales) {
            LocalDate date = sale.getDateTime().atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
            totals.computeIfPresent(date, (key, value) -> value.add(sale.getTotal()));
        }
        for (MarketplaceOrder order : marketplaceOrders) {
            LocalDate date = order.getOrderAt().atZoneSameInstant(BUSINESS_ZONE).toLocalDate();
            totals.computeIfPresent(date, (key, value) -> value.add(zero(order.getItemsValue())));
        }
        return totals.entrySet().stream()
                .map(entry -> new FinancialDashboardResponse.DailyRevenue(
                        entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private List<FinancialDashboardResponse.RecentSale> buildRecentSales(
            List<Sale> sales, List<MarketplaceOrder> marketplaceOrders) {
        List<FinancialDashboardResponse.RecentSale> recent = new java.util.ArrayList<>();
        sales.forEach(sale -> recent.add(new FinancialDashboardResponse.RecentSale(
                sale.getId().toString(), "#" + sale.getSaleNumber(), sale.getDateTime(),
                sale.getOrigin().name(), sale.getTotal(), sale.getStatus().name())));
        marketplaceOrders.forEach(order -> recent.add(new FinancialDashboardResponse.RecentSale(
                "marketplace-" + order.getId(), order.getExternalOrderId(), order.getOrderAt(),
                order.getProvider() == IntegrationProvider.IFOOD ? "IFOOD" : "FOOD_99",
                zero(order.getItemsValue()), order.getNormalizedStatus())));
        return recent.stream().sorted(java.util.Comparator.comparing(
                FinancialDashboardResponse.RecentSale::dateTime).reversed()).limit(4).toList();
    }

    private DateRange resolveRange(String period, LocalDate startDate, LocalDate endDate) {
        String normalized = period == null || period.isBlank()
                ? (startDate != null || endDate != null ? "custom" : "today")
                : period.trim().toLowerCase();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return switch (normalized) {
            case "today" -> new DateRange(today, today);
            case "7d", "last7days", "last_7_days" -> new DateRange(today.minusDays(6), today);
            case "month" -> new DateRange(today.withDayOfMonth(1), today);
            case "custom" -> {
                if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
                    throw new BusinessException("INVALID_PERIOD",
                            "Período personalizado exige startDate e endDate válidos.",
                            HttpStatus.BAD_REQUEST);
                }
                yield new DateRange(startDate, endDate);
            }
            default -> throw new BusinessException("INVALID_PERIOD",
                    "Período inválido. Use today, 7d, month ou custom.", HttpStatus.BAD_REQUEST);
        };
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
