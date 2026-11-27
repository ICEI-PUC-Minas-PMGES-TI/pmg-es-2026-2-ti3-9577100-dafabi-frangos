package com.dafabi.integrations.application;

import com.dafabi.shared.Quantities;
import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.*;
import com.dafabi.integrations.repository.*;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.domain.InventoryLot;
import com.dafabi.inventory.repository.InventoryLotRepository;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductRepository;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketplaceStockService {
    private final MarketplaceItemSnapshotRepository snapshots;
    private final MarketplaceProductMappingRepository mappings;
    private final MarketplaceStockBaselineRepository baselines;
    private final MarketplaceStockApplicationRepository applications;
    private final ProductRepository products;
    private final ProductStockRepository stocks;
    private final StockMovementRepository movements;
    private final InventoryLotRepository lots;

    public MarketplaceStockService(MarketplaceItemSnapshotRepository snapshots,
            MarketplaceProductMappingRepository mappings, MarketplaceStockBaselineRepository baselines,
            MarketplaceStockApplicationRepository applications, ProductRepository products,
            ProductStockRepository stocks, StockMovementRepository movements, InventoryLotRepository lots) {
        this.snapshots=snapshots; this.mappings=mappings; this.baselines=baselines;
        this.applications=applications; this.products=products; this.stocks=stocks; this.movements=movements; this.lots=lots;
    }

    @Transactional(readOnly=true)
    public List<MarketplaceMappingView> listMappings(IntegrationProvider provider,String storeId) {
        requireStore(storeId);
        List<MarketplaceItemSnapshot> imported=snapshots.findByProviderAndExternalStoreIdOrderByBusinessDateDesc(provider,storeId);
        List<String> names=imported.stream().map(MarketplaceItemSnapshot::getExternalItemName).distinct().toList();
        if(names.isEmpty()) return List.of();
        Map<String,List<MarketplaceProductMapping>> byName=mappings
                .findByProviderAndExternalStoreIdAndExternalItemNameIn(provider,storeId,names).stream()
                .collect(Collectors.groupingBy(MarketplaceProductMapping::getExternalItemName));
        Set<UUID> productIds=byName.values().stream().flatMap(Collection::stream).map(MarketplaceProductMapping::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID,Product> productById=products.findAllById(productIds).stream().collect(Collectors.toMap(Product::getId,Function.identity()));
        return names.stream().sorted(String.CASE_INSENSITIVE_ORDER).map(name -> {
            List<MarketplaceProductMapping> rules=byName.getOrDefault(name,List.of());
            boolean ignored=rules.stream().anyMatch(rule->rule.getProductId()==null);
            List<MarketplaceMappingComponentView> components=rules.stream().filter(rule->rule.getProductId()!=null)
                    .map(rule->{Product product=productById.get(rule.getProductId());return product==null?null:
                            new MarketplaceMappingComponentView(product.getId(),product.getName(),product.getUnit(),rule.getQuantityPerItem());})
                    .filter(Objects::nonNull).toList();
            return new MarketplaceMappingView(provider,storeId,name,ignored,components);
        }).toList();
    }

    @Transactional
    public MarketplaceMappingView saveMapping(IntegrationProvider provider,String storeId,String itemName,MarketplaceMappingRequest request) {
        requireStore(storeId); requireItem(itemName);
        if(request.ignored() && request.components()!=null && !request.components().isEmpty())
            throw bad("INVALID_MAPPING","Um item ignorado não pode ter produtos associados.");
        if(!request.ignored() && (request.components()==null||request.components().isEmpty()))
            throw bad("MAPPING_REQUIRED","Associe ao menos um produto ou marque o item como ignorado.");
        if(request.components()!=null) {
            Set<UUID> ids=new HashSet<>();
            for(MarketplaceMappingRequest.Component component:request.components()) {
                if(!ids.add(component.productId())) throw bad("DUPLICATE_COMPONENT","O mesmo produto aparece mais de uma vez. Some as quantidades em uma linha.");
                Product product=products.findById(component.productId()).orElseThrow(()->bad("PRODUCT_NOT_FOUND","Um produto selecionado não existe mais."));
                if(product.getStatus()!=com.dafabi.products.domain.ProductStatus.ACTIVE) throw bad("PRODUCT_INACTIVE","Ative o produto antes de associá-lo a uma venda.");
            }
        }
        List<MarketplaceProductMapping> current=mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider,storeId,itemName);
        Map<UUID,BigDecimal> nextMap=request.ignored()?Map.of():request.components().stream().collect(Collectors.toMap(
                MarketplaceMappingRequest.Component::productId,MarketplaceMappingRequest.Component::quantityPerItem));
        Map<UUID,BigDecimal> currentMap=current.stream().filter(rule->rule.getProductId()!=null).collect(Collectors.toMap(
                MarketplaceProductMapping::getProductId,MarketplaceProductMapping::getQuantityPerItem));
        if(!currentMap.equals(nextMap)) {
            for(MarketplaceItemSnapshot snapshot:snapshots.findByProviderAndExternalStoreIdAndExternalItemName(provider,storeId,itemName))
                for(UUID productId:currentMap.keySet()) if(applications.existsBySnapshotIdAndProductIdAndAppliedQuantityNot(snapshot.getId(),productId,BigDecimal.ZERO))
                    throw bad("MAPPING_ALREADY_APPLIED","Este item já alterou o estoque. Faça um ajuste manual auditado antes de mudar a composição.");
        }
        mappings.deleteByProviderAndExternalStoreIdAndExternalItemName(provider,storeId,itemName);
        if(request.ignored()) mappings.save(new MarketplaceProductMapping(provider,storeId,itemName,null,null));
        else request.components().forEach(component->mappings.save(new MarketplaceProductMapping(
                provider,storeId,itemName,component.productId(),component.quantityPerItem())));
        return listMappings(provider,storeId).stream().filter(row->row.externalItemName().equals(itemName)).findFirst().orElseThrow();
    }

    @Transactional
    public MarketplaceStockBaseline setBaseline(IntegrationProvider provider,MarketplaceStockBaselineRequest request) {
        requireStore(request.externalStoreId());
        MarketplaceStockBaseline baseline=baselines.findByProviderAndExternalStoreId(provider,request.externalStoreId()).orElse(null);
        if(baseline!=null&&!baseline.getStartDate().equals(request.startDate())) {
            for(MarketplaceItemSnapshot snapshot:snapshots.findByProviderAndExternalStoreIdOrderByBusinessDateDesc(provider,request.externalStoreId()))
                for(MarketplaceProductMapping rule:mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider,request.externalStoreId(),snapshot.getExternalItemName()))
                    if(rule.getProductId()!=null&&applications.existsBySnapshotIdAndProductIdAndAppliedQuantityNot(snapshot.getId(),rule.getProductId(),BigDecimal.ZERO))
                        throw bad("STOCK_BASELINE_LOCKED","A data inicial já foi usada em movimentações. Corrija o estoque por ajuste manual auditado.");
        }
        if(baseline==null)baseline=new MarketplaceStockBaseline(provider,request.externalStoreId(),request.startDate());
        else baseline.setStartDate(request.startDate());
        return baselines.save(baseline);
    }

    @Transactional(readOnly=true)
    public MarketplaceStockBaseline getBaseline(IntegrationProvider provider,String storeId) {
        requireStore(storeId);
        return baselines.findByProviderAndExternalStoreId(provider,storeId).orElse(null);
    }

    @Transactional(readOnly=true)
    public List<MarketplaceStockEffectView> preview(IntegrationProvider provider,String storeId,LocalDate start,LocalDate end) {
        validateRange(storeId,start,end);
        return effectViews(provider,storeId,start,end);
    }

    @Transactional
    public List<MarketplaceStockEffectView> apply(IntegrationProvider provider,MarketplaceStockApplyRequest request) {
        validateRange(request.externalStoreId(),request.startDate(),request.endDate());
        List<MarketplaceItemSnapshot> items=snapshots.findRangeForUpdate(
                provider,request.externalStoreId(),request.startDate(),request.endDate());
        MarketplaceStockBaseline baseline=baselines.findByProviderAndExternalStoreId(provider,request.externalStoreId()).orElse(null);
        if(baseline==null)throw bad("STOCK_BASELINE_REQUIRED","Defina a data inicial do estoque antes de aplicar vendas integradas.");
        // Lock every affected product in a stable order before calculating the preview.
        Set<UUID> productIds=new TreeSet<>();
        for(MarketplaceItemSnapshot item:items) {
            if(item.getBusinessDate().isBefore(baseline.getStartDate())) continue;
            mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider,request.externalStoreId(),item.getExternalItemName())
                    .stream().map(MarketplaceProductMapping::getProductId).filter(Objects::nonNull).forEach(productIds::add);
        }
        productIds.forEach(stocks::findByProductIdForUpdate);
        boolean confirmHistorical=request.confirmHistoricalLots() && request.startDate().equals(request.endDate());
        List<MarketplaceStockEffectView> preview=effectViews(provider,request.externalStoreId(),request.startDate(),request.endDate(),request.confirmReturnedProducts(),confirmHistorical);
        Map<UUID,List<MarketplaceStockEffectView>> effectsByItem=preview.stream().collect(Collectors.groupingBy(MarketplaceStockEffectView::snapshotId));
        for(MarketplaceItemSnapshot item:items) {
            List<MarketplaceStockEffectView> effects=effectsByItem.getOrDefault(item.getId(),List.of());
            if(effects.isEmpty()||effects.stream().anyMatch(effect->!Set.of("PENDING","APPLIED").contains(effect.status()))) continue;
            for(MarketplaceStockEffectView effect:effects)
                if("PENDING".equals(effect.status()))
                    applyOne(item,effect.productId(),effect.reportedQuantity(),request.confirmReturnedProducts());
        }
        return effectViews(provider,request.externalStoreId(),request.startDate(),request.endDate());
    }

    private void applyOne(MarketplaceItemSnapshot item,UUID productId,BigDecimal newReported,boolean confirmReturns) {
        ProductStock stock=stocks.findByProductIdForUpdate(productId)
                .orElseThrow(()->new BusinessException("PRODUCT_STOCK_NOT_FOUND","O produto associado não tem saldo de estoque configurado.",HttpStatus.CONFLICT));
        MarketplaceStockApplication application=applications.findForUpdate(item.getId(),productId)
                .orElseGet(()->new MarketplaceStockApplication(item.getId(),productId,newReported,"PENDING"));
        BigDecimal delta=newReported.subtract(application.getAppliedQuantity());
        if(delta.signum()<0 && !confirmReturns) {
            application.updateReported(newReported,"RETURN_REVIEW"); applications.save(application); return;
        }
        BigDecimal amount=Quantities.of(delta.abs());
        BigDecimal previous=stock.getQuantity();
        if(delta.signum()>0 && previous.compareTo(amount)<0) {
            application.updateReported(newReported,"INSUFFICIENT_STOCK"); applications.save(application); return;
        }
        if(delta.signum()>0) stock.setQuantity(previous.subtract(amount));
        else if(delta.signum()<0) stock.setQuantity(previous.add(amount));
        if(amount.signum()>0) adjustLots(stock.getProduct(), delta.signum(), amount, previous, stock.getQuantity(), item, application);
        application.markApplied(newReported); applications.save(application);
    }

    private void adjustLots(Product product, int direction, BigDecimal amount, BigDecimal aggregateBefore,
                            BigDecimal aggregateAfter, MarketplaceItemSnapshot item, MarketplaceStockApplication application) {
        BigDecimal remaining = amount;
        BigDecimal aggregateBalance = aggregateBefore;
        String origin = item.getProvider() == IntegrationProvider.IFOOD ? "iFood" : "99Food";
        if (direction > 0 && product.isPerishable()) {
            List<InventoryLot> eligible = lots.findAvailableByProductForUpdate(product.getId()).stream()
                    .filter(lot -> !lot.getReceivedAt().isAfter(item.getBusinessDate())
                            && !lot.getExpiry().isBefore(item.getBusinessDate())).toList();
            BigDecimal traceable = eligible.stream().map(InventoryLot::getQuantity).reduce(Quantities.ZERO, BigDecimal::add);
            if (traceable.compareTo(amount) < 0) {
                throw new BusinessException("MARKETPLACE_LOT_REVIEW_REQUIRED",
                        "A baixa não pode ser atribuída com segurança aos lotes válidos na data do relatório. Confira o estoque.",
                        HttpStatus.CONFLICT);
            }
            for (InventoryLot lot : eligible) {
                if (remaining.signum() == 0) break;
                BigDecimal consumed = Quantities.min(remaining, lot.getQuantity());
                lot.setQuantity(lot.getQuantity().subtract(consumed));
                BigDecimal next = aggregateBalance.subtract(consumed);
                movements.save(new StockMovement(product, lot, StockMovementType.INTEGRATED_SALE,
                        aggregateBalance, next, origin, "Relatório de itens · " + item.getExternalItemName(),
                        "Importação de marketplace", "MARKETPLACE_STOCK_APPLICATION", application.getId()));
                aggregateBalance = next;
                remaining = remaining.subtract(consumed);
            }
        } else if (direction < 0 && product.isPerishable()) {
            List<StockMovement> previous = new ArrayList<>();
            previous.addAll(movements.findByReferenceTypeAndReferenceIdAndMovementTypeOrderByOccurredAtAsc(
                    "MARKETPLACE_STOCK_APPLICATION", application.getId(), StockMovementType.INTEGRATED_SALE));
            previous.addAll(movements.findByReferenceTypeAndReferenceIdAndMovementTypeOrderByOccurredAtAsc(
                    "MARKETPLACE_STOCK_APPLICATION", application.getId(), StockMovementType.INTEGRATED_CANCELLATION));
            Map<UUID, LotBalance> balances = new LinkedHashMap<>();
            for (StockMovement movement : previous) {
                if (movement.getLot() == null) continue;
                LotBalance lotBalance = balances.computeIfAbsent(movement.getLot().getId(), ignored -> new LotBalance(movement.getLot()));
                lotBalance.remaining = lotBalance.remaining.subtract(movement.getQuantityDelta());
            }
            for (LotBalance balance : balances.values()) {
                if (remaining.signum() == 0) break;
                BigDecimal restored = Quantities.min(remaining, balance.remaining.max(BigDecimal.ZERO));
                if (restored.signum() == 0) continue;
                balance.lot.setQuantity(balance.lot.getQuantity().add(restored));
                BigDecimal next = aggregateBalance.add(restored);
                movements.save(new StockMovement(product, balance.lot, StockMovementType.INTEGRATED_CANCELLATION,
                        aggregateBalance, next, origin, "Recomposição · " + item.getExternalItemName(),
                        "Cancelamento de venda integrada", "MARKETPLACE_STOCK_APPLICATION", application.getId()));
                aggregateBalance = next;
                balance.remaining = balance.remaining.subtract(restored);
                remaining = remaining.subtract(restored);
            }
        }
        if (remaining.signum() > 0) {
            BigDecimal next = aggregateAfter;
            movements.save(new StockMovement(product, null,
                    direction > 0 ? StockMovementType.INTEGRATED_SALE : StockMovementType.INTEGRATED_CANCELLATION,
                    aggregateBalance, next, origin,
                    direction > 0 ? "Relatório de itens · " + item.getExternalItemName() : "Recomposição · " + item.getExternalItemName(),
                    direction > 0 ? "Importação de marketplace" : "Cancelamento de venda integrada",
                    "MARKETPLACE_STOCK_APPLICATION", application.getId()));
        }
    }

    private static final class LotBalance {
        private final InventoryLot lot;
        private BigDecimal remaining = Quantities.ZERO;
        private LotBalance(InventoryLot lot) { this.lot = lot; }
    }

    private List<MarketplaceStockEffectView> effectViews(IntegrationProvider provider,String storeId,LocalDate start,LocalDate end) {
        return effectViews(provider,storeId,start,end,false,false);
    }

    private List<MarketplaceStockEffectView> effectViews(IntegrationProvider provider,String storeId,LocalDate start,LocalDate end,boolean confirmReturns,boolean confirmHistorical) {
        MarketplaceStockBaseline baseline=baselines.findByProviderAndExternalStoreId(provider,storeId).orElse(null);
        List<MarketplaceItemSnapshot> items=snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(provider,storeId,start,end);
        List<MarketplaceStockEffectView> result=new ArrayList<>();
        Map<UUID,Product> productById=new HashMap<>();
        Map<UUID,BigDecimal> stockByProduct=new HashMap<>();
        Set<UUID> missingStock=new HashSet<>();
        for(MarketplaceItemSnapshot item:items) {
            if (baseline != null && item.getBusinessDate().isBefore(baseline.getStartDate())) {
                result.add(new MarketplaceStockEffectView(item.getId(),provider,storeId,item.getBusinessDate(),item.getExternalItemName(),null,null,
                        item.getSoldQuantity(),BigDecimal.ZERO,BigDecimal.ZERO,"BEFORE_BASELINE","Este registro é anterior ao início do controle de estoque."));
                continue;
            }
            List<MarketplaceProductMapping> rules=mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(provider,storeId,item.getExternalItemName());
            if(rules.isEmpty()) {
                result.add(new MarketplaceStockEffectView(item.getId(),provider,storeId,item.getBusinessDate(),item.getExternalItemName(),null,null,
                        item.getSoldQuantity(),BigDecimal.ZERO,item.getSoldQuantity(),"UNMAPPED","Associe o item a um produto antes de movimentar o estoque."));
                continue;
            }
            if(rules.stream().anyMatch(rule->rule.getProductId()==null)) {
                result.add(new MarketplaceStockEffectView(item.getId(),provider,storeId,item.getBusinessDate(),item.getExternalItemName(),null,null,
                        item.getSoldQuantity(),BigDecimal.ZERO,BigDecimal.ZERO,"IGNORED","Este item foi marcado para não movimentar estoque."));
                continue;
            }
            for(MarketplaceProductMapping rule:rules) {
                Product product=productById.computeIfAbsent(rule.getProductId(),id->products.findById(id).orElse(null));
                BigDecimal reported=item.getSoldQuantity().multiply(rule.getQuantityPerItem());
                MarketplaceStockApplication app=applications.findBySnapshotIdAndProductId(item.getId(),rule.getProductId()).orElse(null);
                BigDecimal applied=app==null?BigDecimal.ZERO:app.getAppliedQuantity();
                BigDecimal delta=reported.subtract(applied);
                BigDecimal currentStock=stockByProduct.computeIfAbsent(rule.getProductId(),id->{
                    Optional<ProductStock> stock=stocks.findById(id);
                    if(stock.isEmpty()) missingStock.add(id);
                    return stock.map(ProductStock::getQuantity).orElse(Quantities.ZERO);
                });
                String status; String explanation;
                if(baseline==null){status="BASELINE_REQUIRED";explanation="Defina a data inicial e faça a contagem física antes de aplicar.";}
                else if(item.getBusinessDate().isBefore(baseline.getStartDate())){status="BEFORE_BASELINE";explanation="Este registro é anterior ao início do controle de estoque.";}
                else if(reported.compareTo(applied)==0){status="APPLIED";explanation="O saldo do estoque já corresponde a este relatório.";}
                else if(product==null || missingStock.contains(rule.getProductId())){status="PRODUCT_REVIEW";explanation="O produto associado ou seu saldo de estoque não está disponível.";}
                else if(delta.signum()>0 && product!=null && product.isPerishable() && item.getBusinessDate().isAfter(LocalDate.now(ZoneId.of("America/Sao_Paulo")))){status="FUTURE_REVIEW";explanation="A data da venda está no futuro. Confira o relatório.";}
                else if(delta.signum()>0 && product!=null && product.isPerishable() && !confirmHistorical && item.getBusinessDate().isBefore(LocalDate.now(ZoneId.of("America/Sao_Paulo")))){status="HISTORICAL_REVIEW";explanation="Relatório de outra data: a atribuição aos lotes precisa de conferência; o saldo atual não comprova o estoque daquele dia.";}
                else if(app!=null&&"INSUFFICIENT_STOCK".equals(app.getStatus())&&delta.signum()>0&&currentStock.compareTo(delta)<0){status=app.getStatus();explanation="O saldo atual é insuficiente; ajuste o estoque e aplique novamente.";}
                else if(delta.signum()<0 && !confirmReturns){status="RETURN_REVIEW";explanation="A quantidade do relatório diminuiu; confirme se os produtos foram devolvidos ao estoque.";}
                else {status="PENDING";explanation=reported.compareTo(applied)<0?"Confirme a devolução física antes de recompor o estoque.":"A baixa será aplicada após confirmar a revisão.";}
                result.add(new MarketplaceStockEffectView(item.getId(),provider,storeId,item.getBusinessDate(),item.getExternalItemName(),rule.getProductId(),
                        product==null?"Produto removido":product.getName(),reported,applied,reported.subtract(applied),status,explanation));
            }
        }
        Map<UUID,BigDecimal> lotBalance=new HashMap<>();
        Map<UUID,List<Integer>> indicesByItem=new LinkedHashMap<>();
        for(int i=0;i<result.size();i++)
            indicesByItem.computeIfAbsent(result.get(i).snapshotId(),ignored->new ArrayList<>()).add(i);
        for(List<Integer> indices:indicesByItem.values()) {
            boolean blocked=indices.stream().map(result::get).anyMatch(effect->!Set.of("PENDING","APPLIED").contains(effect.status()));
            for(int index:indices) {
                MarketplaceStockEffectView effect=result.get(index);
                if(!"PENDING".equals(effect.status())||effect.quantityToApply().signum()<=0) continue;
                BigDecimal quantity=effect.quantityToApply();
                String reason=null;
                if(stockByProduct.getOrDefault(effect.productId(),Quantities.ZERO).compareTo(quantity)<0) reason="INSUFFICIENT_STOCK";
                Product product=productById.get(effect.productId());
                if(reason==null && product!=null && product.isPerishable()) {
                    BigDecimal available=lotBalance.computeIfAbsent(effect.productId(),id->lots.findAvailableByProduct(id).stream()
                            .filter(lot->!lot.getReceivedAt().isAfter(effect.businessDate())&&!lot.getExpiry().isBefore(effect.businessDate()))
                            .map(InventoryLot::getQuantity).reduce(Quantities.ZERO,BigDecimal::add));
                    if(available.compareTo(quantity)<0) reason="LOT_REVIEW";
                }
                if(reason!=null) {
                    blocked=true;
                    result.set(index,withStatus(effect,reason,reason.equals("LOT_REVIEW")
                            ? "Os lotes válidos não cobrem esta baixa; confira o estoque."
                            : "O saldo disponível não cobre esta baixa."));
                }
            }
            if(blocked) {
                for(int index:indices) {
                    MarketplaceStockEffectView effect=result.get(index);
                    if("PENDING".equals(effect.status()))
                        result.set(index,withStatus(effect,"ITEM_REVIEW","Outro componente deste item precisa de conferência."));
                }
                continue;
            }
            for(int index:indices) {
                MarketplaceStockEffectView effect=result.get(index);
                if(!"PENDING".equals(effect.status())||effect.quantityToApply().signum()<=0) continue;
                BigDecimal quantity=effect.quantityToApply();
                stockByProduct.compute(effect.productId(),(id,value)->value.subtract(quantity));
                if(lotBalance.containsKey(effect.productId()))
                    lotBalance.compute(effect.productId(),(id,value)->value.subtract(quantity));
            }
        }
        return result;
    }

    private MarketplaceStockEffectView withStatus(MarketplaceStockEffectView effect,String status,String explanation) {
        return new MarketplaceStockEffectView(effect.snapshotId(),effect.provider(),effect.externalStoreId(),effect.businessDate(),
                effect.externalItemName(),effect.productId(),effect.productName(),effect.reportedQuantity(),effect.appliedQuantity(),
                effect.quantityToApply(),status,explanation);
    }

    private void validateRange(String storeId,LocalDate start,LocalDate end){requireStore(storeId);if(start==null||end==null||start.isAfter(end))throw bad("INVALID_PERIOD","Informe um período válido para consultar o estoque integrado.");}
    private void requireStore(String value){if(value==null||value.isBlank())throw bad("STORE_REQUIRED","Informe o identificador da loja no portal.");}
    private void requireItem(String value){if(value==null||value.isBlank()||value.length()>255)throw bad("ITEM_REQUIRED","Nome do item inválido.");}
    private BusinessException bad(String code,String message){return new BusinessException(code,message,HttpStatus.BAD_REQUEST);}
}
