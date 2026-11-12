package com.dafabi.integrations.application;

import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.*;
import com.dafabi.integrations.repository.*;
import com.dafabi.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class IntegrationImportService {
    private final MarketplaceReportParser parser;
    private final IntegrationImportBatchRepository batchRepository;
    private final IntegrationImportRowRepository rowRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplaceItemSnapshotRepository itemRepository;
    private final ObjectMapper objectMapper;
    private final long maxFileBytes;

    public IntegrationImportService(MarketplaceReportParser parser, IntegrationImportBatchRepository batchRepository,
            IntegrationImportRowRepository rowRepository, MarketplaceOrderRepository orderRepository,
            MarketplaceItemSnapshotRepository itemRepository, ObjectMapper objectMapper,
            @Value("${app.integrations.max-file-bytes:10485760}") long maxFileBytes) {
        this.parser=parser; this.batchRepository=batchRepository; this.rowRepository=rowRepository;
        this.orderRepository=orderRepository; this.itemRepository=itemRepository; this.objectMapper=objectMapper;
        this.maxFileBytes=maxFileBytes;
    }

    @Transactional
    public ImportBatchResponse upload(IntegrationProvider provider, MultipartFile file) {
        if(file==null||file.isEmpty())throw bad("EMPTY_FILE","Selecione um arquivo para importar.");
        if(file.getSize()>maxFileBytes)throw bad("FILE_TOO_LARGE","O arquivo excede o limite de 10 MB.");
        String fileName=Optional.ofNullable(file.getOriginalFilename()).orElse("relatorio.xlsx");
        String lower=fileName.toLowerCase(Locale.ROOT);
        if(!lower.endsWith(".xlsx")&&!lower.endsWith(".zip"))throw bad("UNSUPPORTED_FILE","Envie um arquivo XLSX ou ZIP do relatório.");
        byte[] content;
        try{content=file.getBytes();}catch(IOException ex){throw bad("FILE_READ_ERROR","Não foi possível ler o arquivo enviado.");}
        String hash=sha256(content);
        Optional<IntegrationImportBatch> prior=batchRepository.findByProviderAndFileSha256(provider,hash);
        if(prior.isPresent())return toResponse(prior.get(),prior.get().getStatus()!=ImportBatchStatus.READY);

        ParsedReport report=parser.parse(provider,fileName,content);
        List<StagedReportRow> staged=markDuplicates(provider,report);
        int valid=(int)staged.stream().filter(r->r.valid()&&!r.duplicateInFile()).count();
        int invalid=staged.size()-(int)staged.stream().filter(r->r.valid()).count();
        int duplicates=(int)staged.stream().filter(r->r.duplicateInFile()||r.duplicateInSystem()).count();
        if(valid==0)throw bad("NO_VALID_ROWS","O arquivo não contém linhas válidas para importar.");
        ImportSummary summary=summarize(provider,report,staged,duplicates,invalid);
        UUID batchId=UUID.randomUUID();
        IntegrationImportBatch batch=new IntegrationImportBatch(batchId,provider,report.reportType(),fileName,hash,
                report.start(),report.end(),ImportBatchStatus.READY,staged.size(),valid,invalid,duplicates,
                json(summary),OffsetDateTime.now());
        batchRepository.save(batch);
        for(StagedReportRow row:staged)rowRepository.save(new IntegrationImportRow(UUID.randomUUID(),batchId,
                row.rowNumber(),row.rowKey(),row.valid(),row.duplicateInFile(),row.duplicateInSystem(),
                json(row.issues()),json(row.payload())));
        return toResponse(batch,false);
    }

    @Transactional
    public ImportBatchResponse confirm(UUID id) {
        IntegrationImportBatch batch=batchRepository.findByIdForUpdate(id).orElseThrow(()->new BusinessException("IMPORT_NOT_FOUND","Lote de importação não encontrado.",HttpStatus.NOT_FOUND));
        if(batch.getStatus()!=ImportBatchStatus.READY)return toResponse(batch,true);
        List<IntegrationImportRow> rows=rowRepository.findByBatchIdOrderByRowNumber(id);
        OffsetDateTime now=OffsetDateTime.now();
        for(IntegrationImportRow row:rows){
            if(!row.isValid()||row.isDuplicateInFile())continue;
            try {
                if(batch.getReportType()==IntegrationReportType.ORDERS)persistOrder(batch,row,now);
                else persistItem(batch,row,now);
            } catch(JsonProcessingException ex){throw new BusinessException("IMPORT_ROW_INVALID","Uma linha do lote não pôde ser gravada.",HttpStatus.UNPROCESSABLE_ENTITY);}
        }
        batch.markImported(batch.getInvalidRowCount()>0?ImportBatchStatus.IMPORTED_WITH_ERRORS:ImportBatchStatus.IMPORTED,now);
        return toResponse(batchRepository.save(batch),false);
    }

    @Transactional(readOnly=true)
    public ImportBatchResponse get(UUID id){return toResponse(batchRepository.findById(id).orElseThrow(()->new BusinessException("IMPORT_NOT_FOUND","Lote de importação não encontrado.",HttpStatus.NOT_FOUND)),false);}

    @Transactional(readOnly=true)
    public List<ImportBatchResponse> history(IntegrationProvider provider){return batchRepository.findTop50ByProviderOrderByCreatedAtDesc(provider).stream().map(b->toResponse(b,false)).toList();}

    @Transactional(readOnly=true)
    public List<MarketplaceOrderView> orders(IntegrationProvider provider, LocalDate startDate, LocalDate endDate){
        if(startDate != null && endDate != null && startDate.isAfter(endDate)) throw bad("INVALID_PERIOD", "A data inicial deve ser anterior ou igual à data final.");
        ZoneId zone=ZoneId.of("America/Sao_Paulo");
        OffsetDateTime start=(startDate==null?LocalDate.of(1900,1,1):startDate).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end=(endDate==null?LocalDate.of(9999,12,30):endDate).plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return orderRepository.findByProviderAndOrderAtGreaterThanEqualAndOrderAtLessThanOrderByOrderAtDesc(provider,start,end).stream().map(o->new MarketplaceOrderView(o.getId(),o.getProvider(),o.getExternalStoreId(),o.getExternalOrderId(),o.getOrderAt(),o.getRawStatus(),o.getNormalizedStatus(),o.getItemsValue(),o.getCustomerPaid(),o.getFeesAndCommissions(),o.getNetReported(),o.getPaymentMethodRaw(),o.getMarketingCost(),o.getLogisticsCost(),o.getRefundAmount(),o.getFreeDeliveryCost(),o.getTipAmount(),o.getOriginalItemCount(),o.getCompletedAt(),o.getCancelledAt(),o.getCancellationResponsible(),o.getCancellationReason())).toList();}

    @Transactional(readOnly=true)
    public List<MarketplaceItemView> items(IntegrationProvider provider){return itemRepository.findByProviderOrderByBusinessDateDesc(provider).stream().map(i->new MarketplaceItemView(i.getId(),i.getProvider(),i.getExternalStoreId(),i.getBusinessDate(),i.getExternalItemName(),i.getItemRevenue(),i.getSoldQuantity())).toList();}

    @Transactional(readOnly=true)
    public List<IntegrationOverviewResponse> overview(){return Arrays.stream(IntegrationProvider.values()).map(p->{
        OffsetDateTime last=batchRepository.findTop50ByProviderOrderByCreatedAtDesc(p).stream().filter(b->b.getStatus()!=ImportBatchStatus.READY).map(IntegrationImportBatch::getImportedAt).filter(Objects::nonNull).findFirst().orElse(null);
        return new IntegrationOverviewResponse(p,last,orderRepository.countByProvider(p),itemRepository.countByProvider(p));
    }).toList();}

    private List<StagedReportRow> markDuplicates(IntegrationProvider provider,ParsedReport report){
        Map<String,String> seen=new HashMap<>();List<StagedReportRow> out=new ArrayList<>();
        for(StagedReportRow row:report.rows()){
            boolean duplicateInFile=false,duplicateInSystem=false,valid=row.valid();List<String> issues=new ArrayList<>(row.issues());
            String key=row.rowKey();String signature=json(row.payload());
            if(valid){
                String old=seen.putIfAbsent(key,signature);
                if(old!=null){duplicateInFile=true;if(!old.equals(signature))throw bad("CONFLICTING_ROWS", "O arquivo contém linhas diferentes para o mesmo pedido ou item e dia. Confira o relatório antes de importar.");}
            }
            if(report.reportType()==IntegrationReportType.ORDERS&&valid){
                if(!duplicateInFile){ParsedOrderRow p=(ParsedOrderRow)row.payload();duplicateInSystem=orderRepository.existsByProviderAndExternalStoreIdAndExternalOrderId(provider,p.externalStoreId(),p.externalOrderId());}
            }else if(report.reportType()==IntegrationReportType.ITEMS&&valid){
                ParsedItemRow p=(ParsedItemRow)row.payload();duplicateInSystem=itemRepository.findByProviderAndExternalStoreIdAndBusinessDateAndExternalItemName(provider,p.externalStoreId(),p.businessDate(),p.externalItemName()).isPresent();
            }
            out.add(new StagedReportRow(row.rowNumber(),key,valid,duplicateInFile,duplicateInSystem,issues,row.payload()));
        }
        return out;
    }

    private ImportSummary summarize(IntegrationProvider provider,ParsedReport report,List<StagedReportRow> rows,int duplicates,int invalid){
        BigDecimal gross=BigDecimal.ZERO,paid=BigDecimal.ZERO,itemRevenue=BigDecimal.ZERO,itemQuantity=BigDecimal.ZERO;
        BigDecimal ifoodPromo=BigDecimal.ZERO,storePromo=BigDecimal.ZERO,service=BigDecimal.ZERO,commissions=BigDecimal.ZERO,net=BigDecimal.ZERO;
        int completed=0,cancelled=0,partial=0;
        boolean food99FeesKnown=true;
        for(StagedReportRow row:rows){
            if(!row.valid()||row.duplicateInFile())continue;
            if(report.reportType()==IntegrationReportType.ORDERS){ParsedOrderRow p=(ParsedOrderRow)row.payload();
                if(p.itemsValue()!=null)gross=gross.add(p.itemsValue());if(p.customerPaid()!=null)paid=paid.add(p.customerPaid());
                if(p.ifoodPromotion()!=null)ifoodPromo=ifoodPromo.add(p.ifoodPromotion());if(p.storePromotion()!=null)storePromo=storePromo.add(p.storePromotion());
                if(p.serviceFee()!=null)service=service.add(p.serviceFee());
                if(p.feesAndCommissions()!=null)commissions=commissions.add(p.feesAndCommissions());
                else if(provider==IntegrationProvider.FOOD_99)food99FeesKnown=false;
                if(p.netReported()!=null)net=net.add(p.netReported());
                switch(p.normalizedStatus()){case "COMPLETED"->completed++;case "CANCELLED"->cancelled++;case "PARTIALLY_CANCELLED"->partial++;default->{}}
            }else{ParsedItemRow p=(ParsedItemRow)row.payload();if(p.itemRevenue()!=null)itemRevenue=itemRevenue.add(p.itemRevenue());if(p.soldQuantity()!=null)itemQuantity=itemQuantity.add(p.soldQuantity());}
        }
        return new ImportSummary(rows.size(),completed,cancelled,partial,gross,paid,itemRevenue,itemQuantity,ifoodPromo,storePromo,service,food99FeesKnown?commissions:null,net,duplicates,invalid);
    }

    private void persistOrder(IntegrationImportBatch batch,IntegrationImportRow row,OffsetDateTime now)throws JsonProcessingException{
        ParsedOrderRow p=objectMapper.readValue(row.getPayloadJson(),ParsedOrderRow.class);
        Map<String,String> source=p.sourceValues();
        OffsetDateTime completedAt=sourceDateTime(source,"HORARIO DA CONCLUSAO");
        OffsetDateTime cancelledAt=sourceDateTime(source,"HORARIO DO CANCELAMENTO");
        if(cancelledAt==null)cancelledAt=sourceDateTime(source,"DATA DO CANCELAMENTO");
        MarketplaceOrder next=new MarketplaceOrder(UUID.randomUUID(),batch.getProvider(),p.externalStoreId(),p.storeName(),p.externalOrderId(),p.orderAt(),p.rawStatus(),p.normalizedStatus(),p.shiftName(),p.itemsValue(),p.customerPaid(),p.customerDeliveryFee(),p.ifoodPromotion(),p.storePromotion(),p.networkPromotion(),p.serviceFee(),p.feesAndCommissions(),p.netReported(),p.paymentMethodRaw(),p.deliveryType(),p.logisticsProduct(),p.salesChannel(),sourceMoney(source,"DESPESAS DE MARKETING"),sourceMoney(source,"CUSTOS LOGISTICOS"),sourceMoney(source,"VALOR DO REEMBOLSO"),sourceMoney(source,"CUSTO LIQUIDO DA LOJA NA OFERTA DE ENTREGA GRATIS"),sourceMoney(source,"GORJETA"),sourceMoney(source,"CONTAGEM DO ITEM"),completedAt,cancelledAt,sourceValue(source,"PARTE RESPONSAVEL PELO CANCELAMENTO"),sourceValue(source,"MOTIVOS DE CANCELAMENTOS POR PARTE DA LOJA"),batch.getId(),json(source),now);
        Optional<MarketplaceOrder> existing=orderRepository.findByProviderAndExternalStoreIdAndExternalOrderId(batch.getProvider(),p.externalStoreId(),p.externalOrderId());
        if(existing.isPresent()){existing.get().replaceFrom(next);orderRepository.save(existing.get());}else orderRepository.save(next);
    }
    private void persistItem(IntegrationImportBatch batch,IntegrationImportRow row,OffsetDateTime now)throws JsonProcessingException{
        ParsedItemRow p=objectMapper.readValue(row.getPayloadJson(),ParsedItemRow.class);
        MarketplaceItemSnapshot next=new MarketplaceItemSnapshot(UUID.randomUUID(),batch.getProvider(),p.externalStoreId(),p.storeName(),p.businessDate(),p.externalItemName(),p.normalizedItemName(),p.itemRevenue(),p.soldQuantity(),batch.getId(),json(p.sourceValues()),now);
        Optional<MarketplaceItemSnapshot> existing=itemRepository.findByProviderAndExternalStoreIdAndBusinessDateAndExternalItemName(batch.getProvider(),p.externalStoreId(),p.businessDate(),p.externalItemName());
        if(existing.isPresent()){existing.get().replaceFrom(next);itemRepository.save(existing.get());}else itemRepository.save(next);
    }
    private ImportBatchResponse toResponse(IntegrationImportBatch batch,boolean alreadyImported){
        try{
            ImportSummary summary=objectMapper.readValue(batch.getSummaryJson(),ImportSummary.class);
            List<ImportRowPreview> rows=rowRepository.findByBatchIdOrderByRowNumber(batch.getId()).stream().map(r->{
                try{JsonNode p=objectMapper.readTree(r.getPayloadJson());List<String> issues=objectMapper.readValue(r.getIssuesJson(),objectMapper.getTypeFactory().constructCollectionType(List.class,String.class));
                    String date=p.hasNonNull("businessDate")?p.get("businessDate").asText():p.hasNonNull("orderAt")?p.get("orderAt").asText().substring(0,10):null;
                    return new ImportRowPreview(r.getRowNumber(),text(p,"externalOrderId"),date==null?null:java.time.LocalDate.parse(date),text(p,"rawStatus"),text(p,"externalItemName"),decimal(p,"soldQuantity"),decimal(p,"itemsValue")!=null?decimal(p,"itemsValue"):decimal(p,"itemRevenue"),decimal(p,"netReported"),r.isValid(),r.isDuplicateInFile(),r.isDuplicateInSystem(),issues);
                }catch(Exception ex){return new ImportRowPreview(r.getRowNumber(),r.getRowKey(),null,null,null,null,null,null,false,r.isDuplicateInFile(),r.isDuplicateInSystem(),List.of("Não foi possível ler a prévia desta linha."));}
            }).toList();
            return new ImportBatchResponse(batch.getId(),batch.getProvider(),batch.getReportType(),batch.getFileName(),batch.getPeriodStart(),batch.getPeriodEnd(),batch.getStatus(),batch.getRowCount(),batch.getValidRowCount(),batch.getInvalidRowCount(),batch.getDuplicateRowCount(),summary,rows,batch.getCreatedAt(),batch.getImportedAt(),alreadyImported);
        }catch(Exception ex){throw new BusinessException("IMPORT_PREVIEW_ERROR","Não foi possível carregar a prévia do lote.",HttpStatus.INTERNAL_SERVER_ERROR);}
    }
    private String text(JsonNode node,String name){JsonNode v=node.get(name);return v==null||v.isNull()?null:v.asText();}
    private BigDecimal decimal(JsonNode node,String name){JsonNode v=node.get(name);return v==null||v.isNull()?null:v.decimalValue();}
    private String sourceValue(Map<String,String> source,String name){return source.getOrDefault(MarketplaceReportParser.normalizeHeader(name),"");}
    private BigDecimal sourceMoney(Map<String,String> source,String name){String value=sourceValue(source,name);if(value.isBlank())return null;String normalized=value.replace("R$","").replace(" ","").trim();if(normalized.contains(","))normalized=normalized.replace(".","").replace(",",".");try{return new BigDecimal(normalized);}catch(NumberFormatException ex){return null;}}
    private OffsetDateTime sourceDateTime(Map<String,String> source,String name){String value=sourceValue(source,name);if(value.isBlank())return null;for(java.time.format.DateTimeFormatter formatter:List.of(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))try{return java.time.LocalDateTime.parse(value,formatter).atZone(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime();}catch(java.time.format.DateTimeParseException ignored){}try{return OffsetDateTime.parse(value);}catch(java.time.format.DateTimeParseException ignored){return null;}}
    private String json(Object value){try{return objectMapper.writeValueAsString(value);}catch(JsonProcessingException ex){throw new BusinessException("IMPORT_SERIALIZATION_ERROR","Não foi possível preparar os dados do relatório.",HttpStatus.INTERNAL_SERVER_ERROR);}}
    private String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    private BusinessException bad(String code,String message){return new BusinessException(code,message,HttpStatus.BAD_REQUEST);}
}
