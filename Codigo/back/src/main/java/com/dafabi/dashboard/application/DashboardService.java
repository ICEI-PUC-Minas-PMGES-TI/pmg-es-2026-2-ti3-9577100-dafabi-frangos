package com.dafabi.dashboard.application;

import com.dafabi.dashboard.dto.*;
import com.dafabi.expenses.domain.Expense;
import com.dafabi.expenses.domain.ExpenseStatus;
import com.dafabi.expenses.repository.ExpenseRepository;
import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.repository.MarketplaceOrderRepository;
import com.dafabi.integrations.repository.MarketplaceSettlementRepository;
import com.dafabi.sales.domain.*;
import com.dafabi.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {
    private static final ZoneId STORE_ZONE=ZoneId.of("America/Sao_Paulo");
    private final SaleRepository sales;
    private final ExpenseRepository expenses;
    private final MarketplaceOrderRepository orders;
    private final MarketplaceSettlementRepository settlements;

    public DashboardService(SaleRepository sales,ExpenseRepository expenses,MarketplaceOrderRepository orders,MarketplaceSettlementRepository settlements){
        this.sales=sales;this.expenses=expenses;this.orders=orders;this.settlements=settlements;
    }

    @Transactional(readOnly=true)
    public DashboardResponse summary(LocalDate start,LocalDate end){
        if(start==null||end==null||start.isAfter(end)||ChronoUnit.DAYS.between(start,end)>366)
            throw new com.dafabi.shared.exception.BusinessException("INVALID_PERIOD","Informe um período de até 367 dias.");
        OffsetDateTime from=start.atStartOfDay(STORE_ZONE).toOffsetDateTime();
        OffsetDateTime until=end.plusDays(1).atStartOfDay(STORE_ZONE).toOffsetDateTime();
        List<Sale> saleRows=sales.findByDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeDesc(from,until).stream()
                .filter(s->s.getStatus()==SaleStatus.COMPLETED).toList();
        List<MarketplaceOrder> marketRows=new ArrayList<>();
        for(IntegrationProvider provider:IntegrationProvider.values()) marketRows.addAll(
                orders.findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThan(provider,from,until));
        List<MarketplaceOrder> recognized=marketRows.stream().filter(o->"COMPLETED".equals(o.getNormalizedStatus())).toList();
        List<Expense> expenseRows=expenses.findByDateBetween(start,end).stream().filter(e->e.getStatus()!=ExpenseStatus.CANCELLED).toList();
        BigDecimal revenue=saleRows.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add)
                .add(recognized.stream().map(MarketplaceOrder::getItemsValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add));
        BigDecimal expenseTotal=expenseRows.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal ifoodFees=recognized.stream().filter(o->o.getProvider()==IntegrationProvider.IFOOD)
                .map(o->zero(o.getFeesAndCommissions()).add(zero(o.getServiceFee()))).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal food99Fees=recognized.stream().filter(o->o.getProvider()==IntegrationProvider.FOOD_99)
                .map(o->o.getFeesAndCommissions()).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal marketplaceOther=recognized.stream().map(o->
                zero(o.getMarketingCost()).add(zero(o.getFreeDeliveryCost()))
                        .add(zero(o.getLogisticsCost())).add(zero(o.getStorePromotion())).add(zero(o.getRefundAmount())))
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal payoutTotal=settlements.findByPayoutDateBetweenAndStatus(start,end,"PAGO").stream()
                .map(s->s.getAmountReceived()).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        boolean feesComplete=recognized.stream().allMatch(o->o.getFeesAndCommissions()!=null
                && (o.getProvider()==IntegrationProvider.FOOD_99
                    ? o.getMarketingCost()!=null&&o.getFreeDeliveryCost()!=null&&o.getLogisticsCost()!=null&&o.getRefundAmount()!=null
                    : o.getServiceFee()!=null&&o.getStorePromotion()!=null));
        int counter=(int)saleRows.stream().filter(s->s.getOrigin()==SaleOrigin.COUNTER).count();
        int ifood=(int)(saleRows.stream().filter(s->s.getOrigin()==SaleOrigin.IFOOD).count()+recognized.stream().filter(o->o.getProvider()==IntegrationProvider.IFOOD).count());
        int food99=(int)(saleRows.stream().filter(s->s.getOrigin()==SaleOrigin.FOOD_99).count()+recognized.stream().filter(o->o.getProvider()==IntegrationProvider.FOOD_99).count());
        BigDecimal result=revenue.subtract(expenseTotal).subtract(ifoodFees).subtract(food99Fees).subtract(marketplaceOther);
        DashboardSummaryResponse summary=new DashboardSummaryResponse(revenue,expenseTotal,ifoodFees,food99Fees,marketplaceOther,payoutTotal,result,
                counter+ifood+food99,counter,ifood,food99,feesComplete);
        Map<LocalDate,BigDecimal> byDate=new TreeMap<>();
        for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)) byDate.put(date,BigDecimal.ZERO);
        saleRows.forEach(s->byDate.merge(s.getDateTime().atZoneSameInstant(STORE_ZONE).toLocalDate(),s.getTotal(),BigDecimal::add));
        recognized.forEach(o->{if(o.getItemsValue()!=null)byDate.merge(o.getOrderAt().atZoneSameInstant(STORE_ZONE).toLocalDate(),o.getItemsValue(),BigDecimal::add);});
        DateTimeFormatter label=DateTimeFormatter.ofPattern("dd/MM");
        List<DashboardDailyResponse> daily=byDate.entrySet().stream().map(e->new DashboardDailyResponse(e.getKey(),e.getKey().format(label),e.getValue())).toList();
        List<DashboardRecentSaleResponse> recent=new ArrayList<>();
        saleRows.stream().limit(10).forEach(s->recent.add(new DashboardRecentSaleResponse(s.getId().toString(),"#"+s.getSaleNumber(),s.getDateTime(),
                s.getOrigin().name(),s.getTotal(),s.getStatus().name())));
        recognized.stream().limit(10).forEach(o->recent.add(new DashboardRecentSaleResponse(o.getId().toString(),o.getExternalOrderId(),o.getOrderAt(),
                o.getProvider()==IntegrationProvider.IFOOD?"IFOOD":"99FOOD",o.getNetReported(),o.getNormalizedStatus())));
        recent.sort(Comparator.comparing(DashboardRecentSaleResponse::createdAt).reversed());
        return new DashboardResponse(summary,daily,recent.stream().limit(8).toList());
    }

    private BigDecimal zero(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
}
