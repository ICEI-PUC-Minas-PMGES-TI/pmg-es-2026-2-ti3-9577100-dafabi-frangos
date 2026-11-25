package com.dafabi.integrations;

import com.dafabi.integrations.application.MarketplaceStockService;
import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.MarketplaceStockApplyRequest;
import com.dafabi.integrations.repository.*;
import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.*;
import com.dafabi.products.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MarketplaceStockServiceTest {
    @Test
    void historicalPerishableRequiresConfirmationAndValidLotsForItsDay() {
        var snapshots=mock(MarketplaceItemSnapshotRepository.class);
        var mappings=mock(MarketplaceProductMappingRepository.class);
        var baselines=mock(MarketplaceStockBaselineRepository.class);
        var applications=mock(MarketplaceStockApplicationRepository.class);
        var products=mock(ProductRepository.class);
        var stocks=mock(ProductStockRepository.class);
        var movements=mock(StockMovementRepository.class);
        var lots=mock(InventoryLotRepository.class);
        var service=new MarketplaceStockService(snapshots,mappings,baselines,applications,products,stocks,movements,lots);
        var date=LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        var provider=IntegrationProvider.IFOOD;
        var product=new Product(UUID.randomUUID(),null,"Frango",BigDecimal.TEN,BigDecimal.ONE,
                "UN",null,true,false,ProductStatus.ACTIVE,null,null);
        var stock=new ProductStock(product,5);
        var item=new MarketplaceItemSnapshot(UUID.randomUUID(),provider,"loja","Loja",date,
                "Frango","frango",BigDecimal.TEN,BigDecimal.valueOf(2),UUID.randomUUID(),"{}",OffsetDateTime.now());
        var rule=new MarketplaceProductMapping(provider,"loja","Frango",product.getId(),BigDecimal.ONE);
        var lot=mock(InventoryLot.class);
        when(lot.getQuantity()).thenReturn(5);
        when(lot.getReceivedAt()).thenReturn(date.minusDays(1));
        when(lot.getExpiry()).thenReturn(date);
        when(baselines.findByProviderAndExternalStoreId(provider,"loja"))
                .thenReturn(Optional.of(new MarketplaceStockBaseline(provider,"loja",date)));
        when(snapshots.findRangeForUpdate(provider,"loja",date,date)).thenReturn(List.of(item));
        when(snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(provider,"loja",date,date))
                .thenReturn(List.of(item));
        when(mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider,"loja","Frango")).thenReturn(List.of(rule));
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(stocks.findById(product.getId())).thenReturn(Optional.of(stock));
        when(stocks.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(stock));
        when(lots.findAvailableByProduct(product.getId())).thenReturn(List.of(lot));
        when(lots.findAvailableByProductForUpdate(product.getId())).thenReturn(List.of(lot));

        service.apply(provider,new MarketplaceStockApplyRequest("loja",date,date,false,false));
        assertThat(stock.getQuantity()).isEqualTo(5);
        service.apply(provider,new MarketplaceStockApplyRequest("loja",date,date,false,true));
        assertThat(stock.getQuantity()).isEqualTo(3);
        verify(lot).setQuantity(3);
    }
    @Test
    void historicalItemDoesNotRequireMapping() {
        var snapshots = mock(MarketplaceItemSnapshotRepository.class);
        var mappings = mock(MarketplaceProductMappingRepository.class);
        var baselines = mock(MarketplaceStockBaselineRepository.class);
        var service = new MarketplaceStockService(snapshots, mappings, baselines,
                mock(MarketplaceStockApplicationRepository.class), mock(ProductRepository.class),
                mock(ProductStockRepository.class), mock(StockMovementRepository.class), mock(InventoryLotRepository.class));
        var date = LocalDate.of(2026, 1, 1);
        var item = new MarketplaceItemSnapshot(UUID.randomUUID(), IntegrationProvider.FOOD_99, "loja", "Loja", date,
                "Item", "item", BigDecimal.TEN, BigDecimal.ONE, UUID.randomUUID(), "{}", OffsetDateTime.now());
        when(baselines.findByProviderAndExternalStoreId(IntegrationProvider.FOOD_99, "loja"))
                .thenReturn(Optional.of(new MarketplaceStockBaseline(IntegrationProvider.FOOD_99, "loja", date.plusDays(1))));
        when(snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(
                IntegrationProvider.FOOD_99, "loja", date, date)).thenReturn(List.of(item));
        assertThat(service.preview(IntegrationProvider.FOOD_99, "loja", date, date)).singleElement()
                .satisfies(effect -> assertThat(effect.status()).isEqualTo("BEFORE_BASELINE"));
        verifyNoInteractions(mappings);
    }
    @Test
    void historicalPerishableNeedsReviewButAlreadyAppliedReportRemainsApplied() {
        var snapshots = mock(MarketplaceItemSnapshotRepository.class);
        var mappings = mock(MarketplaceProductMappingRepository.class);
        var baselines = mock(MarketplaceStockBaselineRepository.class);
        var applications = mock(MarketplaceStockApplicationRepository.class);
        var products = mock(ProductRepository.class);
        var stocks = mock(ProductStockRepository.class);
        var service = new MarketplaceStockService(snapshots, mappings, baselines, applications, products, stocks,
                mock(StockMovementRepository.class), mock(InventoryLotRepository.class));
        var date = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        var provider = IntegrationProvider.IFOOD;
        var productId = UUID.randomUUID();
        var product = new Product(productId, null, "Frango", BigDecimal.TEN, BigDecimal.ONE,
                "UN", null, true, true, ProductStatus.ACTIVE, null, null);
        var snapshot = new MarketplaceItemSnapshot(UUID.randomUUID(), provider, "loja", "Loja", date,
                "Frango", "frango", BigDecimal.TEN, BigDecimal.ONE, UUID.randomUUID(), "{}", OffsetDateTime.now());
        var rule = new MarketplaceProductMapping(provider, "loja", "Frango", productId, BigDecimal.ONE);
        when(baselines.findByProviderAndExternalStoreId(provider, "loja"))
                .thenReturn(Optional.of(new MarketplaceStockBaseline(provider, "loja", date.minusDays(1))));
        when(snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(provider, "loja", date, date))
                .thenReturn(List.of(snapshot));
        when(mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider, "loja", "Frango"))
                .thenReturn(List.of(rule));
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(stocks.findById(productId)).thenReturn(Optional.of(new ProductStock(product, 5)));
        assertThat(service.preview(provider, "loja", date, date)).singleElement()
                .satisfies(effect -> assertThat(effect.status()).isEqualTo("HISTORICAL_REVIEW"));
        var applied = new MarketplaceStockApplication(snapshot.getId(), productId, BigDecimal.ONE, "PENDING");
        applied.markApplied(BigDecimal.ONE);
        when(applications.findBySnapshotIdAndProductId(snapshot.getId(), productId)).thenReturn(Optional.of(applied));
        assertThat(service.preview(provider, "loja", date, date)).singleElement()
                .satisfies(effect -> assertThat(effect.status()).isEqualTo("APPLIED"));
    }
}
