package com.dafabi.integrations.controller;

import com.dafabi.integrations.application.IntegrationImportService;
import com.dafabi.integrations.application.MarketplaceStockService;
import com.dafabi.integrations.application.MarketplaceSettlementService;
import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.dto.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/integrations")
public class IntegrationImportController {
    private final IntegrationImportService service;
    private final MarketplaceStockService stockService;
    private final MarketplaceSettlementService settlementService;
    public IntegrationImportController(IntegrationImportService service,MarketplaceStockService stockService,MarketplaceSettlementService settlementService){this.service=service;this.stockService=stockService;this.settlementService=settlementService;}

    @GetMapping("/overview")
    public ResponseEntity<List<IntegrationOverviewResponse>> overview(){return ResponseEntity.ok(service.overview());}

    @PostMapping(value="/{provider}/imports",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResponse> upload(@PathVariable IntegrationProvider provider,
            @RequestPart("file") MultipartFile file){return ResponseEntity.accepted().body(service.upload(provider,file));}

    @GetMapping("/imports/{id}")
    public ResponseEntity<ImportBatchResponse> get(@PathVariable UUID id){return ResponseEntity.ok(service.get(id));}

    @PostMapping("/imports/{id}/confirm")
    public ResponseEntity<ImportBatchResponse> confirm(@PathVariable UUID id){return ResponseEntity.ok(service.confirm(id));}

    @GetMapping("/{provider}/imports")
    public ResponseEntity<List<ImportBatchResponse>> history(@PathVariable IntegrationProvider provider){return ResponseEntity.ok(service.history(provider));}

    @GetMapping("/{provider}/orders")
    public ResponseEntity<List<MarketplaceOrderView>> orders(@PathVariable IntegrationProvider provider,
            @RequestParam(required=false) LocalDate startDate, @RequestParam(required=false) LocalDate endDate){return ResponseEntity.ok(service.orders(provider,startDate,endDate));}

    @GetMapping("/{provider}/items")
    public ResponseEntity<List<MarketplaceItemView>> items(@PathVariable IntegrationProvider provider){return ResponseEntity.ok(service.items(provider));}

    @GetMapping("/{provider}/mappings")
    public ResponseEntity<List<MarketplaceMappingView>> mappings(@PathVariable IntegrationProvider provider,
            @RequestParam String externalStoreId){return ResponseEntity.ok(stockService.listMappings(provider,externalStoreId));}

    @PutMapping("/{provider}/mappings")
    public ResponseEntity<MarketplaceMappingView> saveMapping(@PathVariable IntegrationProvider provider,
            @RequestParam String externalStoreId,@RequestParam String externalItemName,
            @Valid @RequestBody MarketplaceMappingRequest request){
        return ResponseEntity.ok(stockService.saveMapping(provider,externalStoreId,externalItemName,request));
    }

    @PutMapping("/{provider}/stock-baseline")
    public ResponseEntity<MarketplaceStockBaselineView> setStockBaseline(@PathVariable IntegrationProvider provider,
            @Valid @RequestBody MarketplaceStockBaselineRequest request){
        var baseline=stockService.setBaseline(provider,request);
        return ResponseEntity.ok(new MarketplaceStockBaselineView(baseline.getProvider(),baseline.getExternalStoreId(),baseline.getStartDate()));
    }

    @GetMapping("/{provider}/stock-baseline")
    public ResponseEntity<MarketplaceStockBaselineView> getStockBaseline(@PathVariable IntegrationProvider provider,
            @RequestParam String externalStoreId){
        var baseline=stockService.getBaseline(provider,externalStoreId);
        return baseline==null?ResponseEntity.noContent().build():ResponseEntity.ok(
                new MarketplaceStockBaselineView(baseline.getProvider(),baseline.getExternalStoreId(),baseline.getStartDate()));
    }

    @GetMapping("/{provider}/stock-effects")
    public ResponseEntity<List<MarketplaceStockEffectView>> stockEffects(@PathVariable IntegrationProvider provider,
            @RequestParam String externalStoreId,@RequestParam LocalDate startDate,@RequestParam LocalDate endDate){
        return ResponseEntity.ok(stockService.preview(provider,externalStoreId,startDate,endDate));
    }

    @PostMapping("/{provider}/stock-effects/apply")
    public ResponseEntity<List<MarketplaceStockEffectView>> applyStock(@PathVariable IntegrationProvider provider,
            @Valid @RequestBody MarketplaceStockApplyRequest request){
        return ResponseEntity.ok(stockService.apply(provider,request));
    }

    @GetMapping("/{provider}/settlements")
    public ResponseEntity<List<MarketplaceSettlementView>> settlements(@PathVariable IntegrationProvider provider,
            @RequestParam String externalStoreId){return ResponseEntity.ok(settlementService.list(provider,externalStoreId));}

    @PutMapping("/{provider}/settlements")
    public ResponseEntity<MarketplaceSettlementView> saveSettlement(@PathVariable IntegrationProvider provider,
            @Valid @RequestBody MarketplaceSettlementRequest request){return ResponseEntity.ok(settlementService.save(provider,request));}
}
