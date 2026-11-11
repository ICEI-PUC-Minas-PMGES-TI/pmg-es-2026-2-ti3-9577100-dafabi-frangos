package com.dafabi.integrations.application;

import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.*;
import com.dafabi.integrations.repository.*;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
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

    public MarketplaceStockService(MarketplaceItemSnapshotRepository snapshots,
            MarketplaceProductMappingRepository mappings, MarketplaceStockBaselineRepository baselines,
            MarketplaceStockApplicationRepository applications, ProductRepository products,
            ProductStockRepository stocks, StockMovementRepository movements) {
        this.snapshots=snapshots; this.mappings=mappings; this.baselines=baselines;
        this.applications=applications; this.products=products; this.stocks=stocks; this.movements=movements;
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
        List<MarketplaceStockEffectView> preview=effectViews(provider,request.externalStoreId(),request.startDate(),request.endDate());
        if(preview.stream().anyMatch(effect->Set.of("UNMAPPED","BASELINE_REQUIRED","UNIT_REVIEW","INSUFFICIENT_STOCK").contains(effect.status())))
            throw bad("STOCK_REVIEW_REQUIRED","Resolva os itens sem associação, unidades fracionadas ou saldo insuficiente antes de aplicar o período.");
        if(!request.confirmReturnedProducts()&&preview.stream().anyMatch(effect->effect.quantityToApply().signum()<0))
            throw bad("RETURN_CONFIRMATION_REQUIRED","Confirme a devolução física dos produtos antes de recompor o estoque.");
        List<MarketplaceItemSnapshot> items=snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(
                provider,request.externalStoreId(),request.startDate(),request.endDate());
        MarketplaceStockBaseline baseline=baselines.findByProviderAndExternalStoreId(provider,request.externalStoreId()).orElse(null);
        if(baseline==null)throw bad("STOCK_BASELINE_REQUIRED","Defina a data inicial do estoque antes de aplicar vendas integradas.");
        Map<UUID,BigDecimal> requiredByProduct=new TreeMap<>();
        for(MarketplaceItemSnapshot item:items) {
            List<MarketplaceProductMapping> rules=mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(
                    provider,request.externalStoreId(),item.getExternalItemName());
            if(rules.isEmpty()||rules.stream().anyMatch(rule->rule.getProductId()==null)) continue;
            if(item.getBusinessDate().isBefore(baseline.getStartDate())) continue;
            Map<UUID,BigDecimal> quantities=new LinkedHashMap<>();
            for(MarketplaceProductMapping rule:rules) quantities.merge(rule.getProductId(),
                    item.getSoldQuantity().multiply(rule.getQuantityPerItem()),BigDecimal::add);
            for(Map.Entry<UUID,BigDecimal> mapped:quantities.entrySet()) {
                MarketplaceStockApplication existing=applications.findBySnapshotIdAndProductId(item.getId(),mapped.getKey()).orElse(null);
                BigDecimal applied=existing==null?BigDecimal.ZERO:existing.getAppliedQuantity();
                BigDecimal delta=mapped.getValue().subtract(applied);
                if(delta.stripTrailingZeros().scale()>0)throw bad("UNIT_REVIEW_REQUIRED","A quantidade calculada é fracionada e precisa ser revisada antes de aplicar.");
                if(delta.signum()>0)requiredByProduct.merge(mapped.getKey(),delta,BigDecimal::add);
            }
        }
        for(Map.Entry<UUID,BigDecimal> required:requiredByProduct.entrySet()) {
            ProductStock stock=stocks.findByProductIdForUpdate(required.getKey()).orElseThrow(
                    ()->new BusinessException("PRODUCT_STOCK_NOT_FOUND","O produto associado não tem saldo de estoque configurado.",HttpStatus.CONFLICT));
            if(BigDecimal.valueOf(stock.getQuantity()).compareTo(required.getValue())<0)
                throw new BusinessException("INSUFFICIENT_STOCK","O estoque disponível não cobre as baixas deste período. Ajuste o saldo e confira novamente.",HttpStatus.CONFLICT);
        }
        for(MarketplaceItemSnapshot item:items) {
            if(item.getBusinessDate().isBefore(baseline.getStartDate()))continue;
            List<MarketplaceProductMapping> rules=mappings.findByProviderAndExternalStoreIdAndExternalItemNameOrderByProductId(
                    provider,request.externalStoreId(),item.getExternalItemName());
            if(rules.isEmpty()||rules.stream().anyMatch(rule->rule.getProductId()==null))continue;
            Map<UUID,BigDecimal> quantities=new LinkedHashMap<>();
            for(MarketplaceProductMapping rule:rules)quantities.merge(rule.getProductId(),item.getSoldQuantity().multiply(rule.getQuantityPerItem()),BigDecimal::add);
            for(Map.Entry<UUID,BigDecimal> mapped:quantities.entrySet())applyOne(item,mapped.getKey(),mapped.getValue(),request.confirmReturnedProducts());
        }
        return effectViews(provider,request.externalStoreId(),request.startDate(),request.endDate());
    }

    private void applyOne(MarketplaceItemSnapshot item,UUID productId,BigDecimal newReported,boolean confirmReturns) {
        if(newReported.stripTrailingZeros().scale()>0) return;
        ProductStock stock=stocks.findByProductIdForUpdate(productId)
                .orElseThrow(()->new BusinessException("PRODUCT_STOCK_NOT_FOUND","O produto associado não tem saldo de estoque configurado.",HttpStatus.CONFLICT));
        MarketplaceStockApplication application=applications.findForUpdate(item.getId(),productId)
                .orElseGet(()->new MarketplaceStockApplication(item.getId(),productId,newReported,"PENDING"));
        BigDecimal delta=newReported.subtract(application.getAppliedQuantity());
        if(delta.signum()<0 && !confirmReturns) {
            application.updateReported(newReported,"RETURN_REVIEW"); applications.save(application); return;
        }
        int amount=delta.abs().intValueExact();
        int previous=stock.getQuantity();
        if(delta.signum()>0 && previous<amount) {
            application.updateReported(newReported,"INSUFFICIENT_STOCK"); applications.save(application); return;
        }
        if(delta.signum()>0) stock.setQuantity(previous-amount);
        else if(delta.signum()<0) stock.setQuantity(previous+amount);
        if(amount>0) movements.save(new StockMovement(stock.getProduct(),null,
                delta.signum()>0?StockMovementType.INTEGRATED_SALE:StockMovementType.INTEGRATED_CANCELLATION,
                previous,stock.getQuantity(),item.getProvider()==IntegrationProvider.IFOOD?"iFood":"99Food",
                "Relatório de itens · "+item.getExternalItemName(),"Importação de marketplace",
                "MARKETPLACE_STOCK_APPLICATION",application.getId()));
        application.markApplied(newReported); applications.save(application);
    }

    private List<MarketplaceStockEffectView> effectViews(IntegrationProvider provider,String storeId,LocalDate start,LocalDate end) {
        MarketplaceStockBaseline baseline=baselines.findByProviderAndExternalStoreId(provider,storeId).orElse(null);
        List<MarketplaceItemSnapshot> items=snapshots.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(provider,storeId,start,end);
        List<MarketplaceStockEffectView> result=new ArrayList<>();
        Map<UUID,Product> productById=new HashMap<>();
        Map<UUID,Integer> stockByProduct=new HashMap<>();
        for(MarketplaceItemSnapshot item:items) {
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
                Integer currentStock=stockByProduct.computeIfAbsent(rule.getProductId(),id->stocks.findById(id).map(ProductStock::getQuantity).orElse(0));
                String status; String explanation;
                if(baseline==null){status="BASELINE_REQUIRED";explanation="Defina a data inicial e faça a contagem física antes de aplicar.";}
                else if(item.getBusinessDate().isBefore(baseline.getStartDate())){status="BEFORE_BASELINE";explanation="Este registro é anterior ao início do controle de estoque.";}
                else if(reported.stripTrailingZeros().scale()>0){status="UNIT_REVIEW";explanation="A quantidade calculada é fracionada e o estoque atual aceita apenas unidades inteiras.";}
                else if(reported.compareTo(applied)==0){status="APPLIED";explanation="O saldo do estoque já corresponde a este relatório.";}
                else if(app!=null&&"INSUFFICIENT_STOCK".equals(app.getStatus())&&delta.signum()>0&&BigDecimal.valueOf(currentStock).compareTo(delta)<0){status=app.getStatus();explanation="O saldo atual é insuficiente; ajuste o estoque e aplique novamente.";}
                else if(app!=null&&"RETURN_REVIEW".equals(app.getStatus())){status=app.getStatus();explanation="A quantidade do relatório diminuiu; confirme se os produtos foram devolvidos ao estoque.";}
                else {status="PENDING";explanation=reported.compareTo(applied)<0?"Confirme a devolução física antes de recompor o estoque.":"A baixa será aplicada após confirmar a revisão.";}
                result.add(new MarketplaceStockEffectView(item.getId(),provider,storeId,item.getBusinessDate(),item.getExternalItemName(),rule.getProductId(),
                        product==null?"Produto removido":product.getName(),reported,applied,reported.subtract(applied),status,explanation));
            }
        }
        Map<UUID,BigDecimal> requiredByProduct=new HashMap<>();
        result.stream().filter(effect->"PENDING".equals(effect.status())&&effect.quantityToApply().signum()>0)
                .forEach(effect->requiredByProduct.merge(effect.productId(),effect.quantityToApply(),BigDecimal::add));
        requiredByProduct.forEach((productId,required)->{
            if(BigDecimal.valueOf(stockByProduct.getOrDefault(productId,0)).compareTo(required)>=0)return;
            for(int i=0;i<result.size();i++){
                MarketplaceStockEffectView effect=result.get(i);
                if(productId.equals(effect.productId())&&"PENDING".equals(effect.status()))
                    result.set(i,new MarketplaceStockEffectView(effect.snapshotId(),effect.provider(),effect.externalStoreId(),effect.businessDate(),
                            effect.externalItemName(),effect.productId(),effect.productName(),effect.reportedQuantity(),effect.appliedQuantity(),
                            effect.quantityToApply(),"INSUFFICIENT_STOCK","O saldo atual não cobre as baixas pendentes deste produto no período."));
            }
        });
        return result;
    }

    private void validateRange(String storeId,LocalDate start,LocalDate end){requireStore(storeId);if(start==null||end==null||start.isAfter(end))throw bad("INVALID_PERIOD","Informe um período válido para consultar o estoque integrado.");}
    private void requireStore(String value){if(value==null||value.isBlank())throw bad("STORE_REQUIRED","Informe o identificador da loja no portal.");}
    private void requireItem(String value){if(value==null||value.isBlank()||value.length()>255)throw bad("ITEM_REQUIRED","Nome do item inválido.");}
    private BusinessException bad(String code,String message){return new BusinessException(code,message,HttpStatus.BAD_REQUEST);}
}
