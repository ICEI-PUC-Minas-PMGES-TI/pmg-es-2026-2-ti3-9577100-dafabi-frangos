package com.dafabi.integrations;

import com.dafabi.integrations.application.MarketplaceStockService;
import com.dafabi.integrations.domain.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MarketplaceStockServiceTest {
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
