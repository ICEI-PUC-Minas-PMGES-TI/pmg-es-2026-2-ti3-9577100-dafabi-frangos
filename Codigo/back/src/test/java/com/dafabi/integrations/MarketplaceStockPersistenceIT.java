package com.dafabi.integrations;

import com.dafabi.integrations.application.MarketplaceStockService;
import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.MarketplaceStockApplyRequest;
import com.dafabi.integrations.repository.*;
import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.products.domain.*;
import com.dafabi.products.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceStockPersistenceIT {
    @Autowired MarketplaceStockService service;
    @Autowired MarketplaceItemSnapshotRepository snapshots;
    @Autowired MarketplaceProductMappingRepository mappings;
    @Autowired MarketplaceStockBaselineRepository baselines;
    @Autowired MarketplaceStockApplicationRepository applications;
    @Autowired ProductRepository products;
    @Autowired ProductStockRepository stocks;
    @Autowired InventoryLotRepository lots;
    @Autowired EntityManager entityManager;

    @Test
    void appliesValidItemWithUnmappedItemAndNeverAppliesItTwice() {
        LocalDate date=LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String store="teste-"+UUID.randomUUID();
        Product product=new Product(UUID.randomUUID(),null,"Produto",BigDecimal.TEN,BigDecimal.ONE,
                "UN",null,false,false,ProductStatus.ACTIVE,null,null);
        product.setStock(new ProductStock(product,5));
        product=products.save(product);
        var good=snapshots.save(new MarketplaceItemSnapshot(UUID.randomUUID(),IntegrationProvider.FOOD_99,store,"Loja",date,
                "Item associado","item associado",BigDecimal.TEN,BigDecimal.valueOf(2),UUID.randomUUID(),"{}",OffsetDateTime.now()));
        snapshots.save(new MarketplaceItemSnapshot(UUID.randomUUID(),IntegrationProvider.FOOD_99,store,"Loja",date,
                "Item sem associação","item sem associação",BigDecimal.TEN,BigDecimal.ONE,UUID.randomUUID(),"{}",OffsetDateTime.now()));
        mappings.save(new MarketplaceProductMapping(IntegrationProvider.FOOD_99,store,"Item associado",product.getId(),BigDecimal.ONE));
        baselines.save(new MarketplaceStockBaseline(IntegrationProvider.FOOD_99,store,date));
        entityManager.flush();

        var request=new MarketplaceStockApplyRequest(store,date,date,false);
        var result=service.apply(IntegrationProvider.FOOD_99,request);
        entityManager.flush();
        assertThat(result).extracting("status").contains("APPLIED","UNMAPPED");
        assertThat(stocks.findById(product.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("3");
        assertThat(applications.findBySnapshotIdAndProductId(good.getId(),product.getId()).orElseThrow().getAppliedQuantity())
                .isEqualByComparingTo("2");
        service.apply(IntegrationProvider.FOOD_99,request);
        entityManager.flush();
        assertThat(stocks.findById(product.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("3");
    }

    @Test
    void confirmedHistoricalSaleUsesOnlyLotsValidOnTheSaleDate() {
        LocalDate date=LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(2);
        String store="teste-"+UUID.randomUUID();
        Product product=new Product(UUID.randomUUID(),null,"Perecível",BigDecimal.TEN,BigDecimal.ONE,
                "UN",null,true,false,ProductStatus.ACTIVE,null,null);
        product.setStock(new ProductStock(product,10));
        product=products.save(product);
        var expired=lots.save(new InventoryLot(product,"vencido",date.minusDays(3),date.minusDays(1),2));
        var valid=lots.save(new InventoryLot(product,"válido",date.minusDays(3),date,4));
        var receivedLater=lots.save(new InventoryLot(product,"posterior",date.plusDays(1),date.plusDays(3),4));
        snapshots.save(new MarketplaceItemSnapshot(UUID.randomUUID(),IntegrationProvider.IFOOD,store,"Loja",date,
                "Frango","frango",BigDecimal.TEN,BigDecimal.valueOf(3),UUID.randomUUID(),"{}",OffsetDateTime.now()));
        mappings.save(new MarketplaceProductMapping(IntegrationProvider.IFOOD,store,"Frango",product.getId(),BigDecimal.ONE));
        baselines.save(new MarketplaceStockBaseline(IntegrationProvider.IFOOD,store,date));
        entityManager.flush();
        var unconfirmed=new MarketplaceStockApplyRequest(store,date,date,false,false);
        assertThat(service.apply(IntegrationProvider.IFOOD,unconfirmed)).extracting("status").containsExactly("HISTORICAL_REVIEW");
        var confirmed=new MarketplaceStockApplyRequest(store,date,date,false,true);
        assertThat(service.apply(IntegrationProvider.IFOOD,confirmed)).extracting("status").containsExactly("APPLIED");
        entityManager.flush();
        assertThat(stocks.findById(product.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("7");
        assertThat(lots.findById(expired.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("2");
        assertThat(lots.findById(valid.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("1");
        assertThat(lots.findById(receivedLater.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("4");
        service.apply(IntegrationProvider.IFOOD,confirmed);
        assertThat(stocks.findById(product.getId()).orElseThrow().getQuantity()).isEqualByComparingTo("7");
    }
}
