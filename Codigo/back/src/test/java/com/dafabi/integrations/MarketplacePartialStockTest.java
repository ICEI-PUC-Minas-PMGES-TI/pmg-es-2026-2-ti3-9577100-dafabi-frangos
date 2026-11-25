package com.dafabi.integrations;

import com.dafabi.integrations.application.MarketplaceStockService;
import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.MarketplaceStockApplyRequest;
import com.dafabi.integrations.repository.*;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.*;
import com.dafabi.products.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketplacePartialStockTest {
    private final MarketplaceItemSnapshotRepository snapshots=mock(MarketplaceItemSnapshotRepository.class);
    private final MarketplaceProductMappingRepository mappings=mock(MarketplaceProductMappingRepository.class);
    private final MarketplaceStockBaselineRepository baselines=mock(MarketplaceStockBaselineRepository.class);
    private final MarketplaceStockApplicationRepository applications=mock(MarketplaceStockApplicationRepository.class);
    private final ProductRepository products=mock(ProductRepository.class);
    private final ProductStockRepository stocks=mock(ProductStockRepository.class);
    private final StockMovementRepository movements=mock(StockMovementRepository.class);
    private final InventoryLotRepository lots=mock(InventoryLotRepository.class);
    private final MarketplaceStockService service=new MarketplaceStockService(snapshots,mappings,baselines,applications,products,stocks,movements,lots);
    private final LocalDate date=LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private final IntegrationProvider provider=IntegrationProvider.FOOD_99;
    private final Map<String,MarketplaceStockApplication> applied=new HashMap<>();

    MarketplacePartialStockTest() {
        when(baselines.findByProviderAndExternalStoreId(provider,"loja"))
                .thenReturn(Optional.of(new MarketplaceStockBaseline(provider,"loja",date)));
        when(applications.findBySnapshotIdAndProductId(any(),any()))
                .thenAnswer(call->Optional.ofNullable(applied.get(call.getArgument(0)+":"+call.getArgument(1))));
        when(applications.findForUpdate(any(),any()))
                .thenAnswer(call->Optional.ofNullable(applied.get(call.getArgument(0)+":"+call.getArgument(1))));
        when(applications.save(any())).thenAnswer(call->{
            MarketplaceStockApplication value=call.getArgument(0);
            applied.put(value.getSnapshotId()+":"+value.getProductId(),value);
            return value;
        });
    }

    private MarketplaceItemSnapshot item(String name,int quantity) {
        return new MarketplaceItemSnapshot(UUID.randomUUID(),provider,"loja","Loja",date,name,name,
                BigDecimal.TEN,BigDecimal.valueOf(quantity),UUID.randomUUID(),"{}",OffsetDateTime.now());
    }
    private ProductStock product(String name,int quantity,boolean perishable) {
        var product=new Product(UUID.randomUUID(),null,name,BigDecimal.TEN,BigDecimal.ONE,"UN",null,
                perishable,true,ProductStatus.ACTIVE,null,null);
        var stock=new ProductStock(product,quantity);
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(stocks.findById(product.getId())).thenReturn(Optional.of(stock));
        when(stocks.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(stock));
        return stock;
    }
    private void rules(String name,ProductStock... components) {
        when(mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider,"loja",name))
                .thenReturn(Arrays.stream(components).map(stock->new MarketplaceProductMapping(provider,"loja",name,
                        stock.getProduct().getId(),BigDecimal.ONE)).toList());
    }
    private void report(MarketplaceItemSnapshot... items) {
        when(snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(provider,"loja",date,date))
                .thenReturn(List.of(items));
        when(snapshots.findRangeForUpdate(provider,"loja",date,date)).thenReturn(List.of(items));
    }
    private void apply(boolean confirmReturns) {
        service.apply(provider,new MarketplaceStockApplyRequest("loja",date,date,confirmReturns));
    }

    @Test
    void validItemsApplyWhileUnmappedAndInsufficientItemsRemainAndRetryIsIdempotent() {
        var stock=product("Produto",3,false);
        var valid=item("A",2); var shortItem=item("B",2); var unmapped=item("C",1);
        rules("A",stock); rules("B",stock); report(valid,shortItem,unmapped);

        var preview=service.preview(provider,"loja",date,date);
        assertThat(preview).extracting("status").containsExactly("PENDING","INSUFFICIENT_STOCK","UNMAPPED");
        apply(false);
        assertThat(stock.getQuantity()).isEqualTo(1);
        assertThat(applied).hasSize(1);
        apply(false);
        assertThat(stock.getQuantity()).isEqualTo(1);
        assertThat(applied).hasSize(1);
    }

    @Test
    void comboIsNotPartiallyConsumedWhenOneComponentIsUnavailable() {
        var first=product("Primeiro",5,false); var second=product("Segundo",0,false);
        var independent=product("Avulso",4,false);
        rules("Combo",first,second); rules("Avulso",independent);
        report(item("Combo",2),item("Avulso",2));

        assertThat(service.preview(provider,"loja",date,date)).extracting("status")
                .containsExactly("ITEM_REVIEW","INSUFFICIENT_STOCK","PENDING");
        apply(false);
        assertThat(first.getQuantity()).isEqualTo(5);
        assertThat(second.getQuantity()).isZero();
        assertThat(independent.getQuantity()).isEqualTo(2);
    }

    @Test
    void unconfirmedReturnDoesNotBlockOtherSales() {
        var returned=product("Devolução",2,false); var sold=product("Venda",4,false);
        var returnItem=item("Devolução",1); var saleItem=item("Venda",2);
        rules("Devolução",returned); rules("Venda",sold); report(returnItem,saleItem);
        var existing=new MarketplaceStockApplication(returnItem.getId(),returned.getProduct().getId(),BigDecimal.valueOf(3),"PENDING");
        existing.markApplied(BigDecimal.valueOf(3));
        applied.put(returnItem.getId()+":"+returned.getProduct().getId(),existing);

        apply(false);
        assertThat(returned.getQuantity()).isEqualTo(2);
        assertThat(sold.getQuantity()).isEqualTo(2);
        apply(true);
        assertThat(returned.getQuantity()).isEqualTo(4);
        assertThat(sold.getQuantity()).isEqualTo(2);
    }

    @Test
    void missingValidLotsDoNotBlockAnIndependentItem() {
        var perishable=product("Perecível",10,true); var independent=product("Avulso",4,false);
        rules("Perecível",perishable); rules("Avulso",independent);
        report(item("Perecível",2),item("Avulso",1));
        assertThat(service.preview(provider,"loja",date,date)).extracting("status").containsExactly("LOT_REVIEW","PENDING");
        apply(false);
        assertThat(perishable.getQuantity()).isEqualTo(10);
        assertThat(independent.getQuantity()).isEqualTo(3);
    }
}
