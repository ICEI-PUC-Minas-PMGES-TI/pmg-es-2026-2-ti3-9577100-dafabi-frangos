package com.dafabi.integrations.application;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.IntegrationReportType;
import com.dafabi.integrations.dto.ImportSummary;
import com.dafabi.shared.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipEntry;

@Component
public class MarketplaceReportParser {
    private static final int MAX_UNCOMPRESSED_BYTES = 20 * 1024 * 1024;
    private static final ZoneId STORE_ZONE = ZoneId.of("America/Sao_Paulo");
    private final DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));

    public ParsedReport parse(IntegrationProvider provider, String fileName, byte[] bytes) {
        byte[] workbookBytes = fileName.toLowerCase(Locale.ROOT).endsWith(".zip") ? unzipWorkbook(bytes) : bytes;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            if (workbook.getSheet("Funil Loja") != null && workbook.getSheet("Itens") != null && workbook.getSheet("Complementos") != null) {
                if (provider != IntegrationProvider.IFOOD) throw invalid("REPORT_PROVIDER_MISMATCH", "Este relatório de cardápio foi reconhecido como iFood.");
                return parseIfoodMenu(workbook);
            }
            Sheet sheet = workbook.getSheetAt(0);
            HeaderRow header = findHeader(sheet);
            if (header == null) throw invalid("UNSUPPORTED_REPORT", "Não reconheci as colunas desse relatório.");
            if (header.has("ID COMPLETO DO PEDIDO")) {
                if (provider != IntegrationProvider.IFOOD) throw invalid("REPORT_PROVIDER_MISMATCH", "Este relatório de pedidos foi reconhecido como iFood.");
                return parseOrders(sheet, header);
            }
            if (header.has("ID DO PEDIDO") && header.has("HORÁRIO DO PEDIDO")) {
                if (provider != IntegrationProvider.FOOD_99) throw invalid("REPORT_PROVIDER_MISMATCH", "Este relatório de pedidos foi reconhecido como 99Food.");
                return parseFood99Orders(sheet, header);
            }
            if (header.has("NOME DO ITEM") && header.has("RECEITA DO ITEM") && header.has("VOLUME DE VENDAS DO ITEM")) {
                if (provider != IntegrationProvider.FOOD_99) throw invalid("REPORT_PROVIDER_MISMATCH", "Este relatório de itens foi reconhecido como 99Food.");
                return parseItems(provider, sheet, header);
            }
            throw invalid("UNSUPPORTED_REPORT", "O arquivo foi aberto, mas o tipo de relatório ainda não é suportado.");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("INVALID_REPORT", "Não foi possível ler o arquivo. Confira se ele é um XLSX válido.", HttpStatus.BAD_REQUEST);
        }
    }

    private ParsedReport parseOrders(Sheet sheet, HeaderRow h) {
        List<StagedReportRow> rows = new ArrayList<>();
        LocalDate min = null, max = null;
        BigDecimal gross=BigDecimal.ZERO, paid=BigDecimal.ZERO, ifoodPromo=BigDecimal.ZERO, storePromo=BigDecimal.ZERO;
        BigDecimal service=BigDecimal.ZERO, commissions=BigDecimal.ZERO, net=BigDecimal.ZERO;
        int completed=0, cancelled=0, partial=0, invalid=0;
        for (int i=h.rowIndex()+1;i<=sheet.getLastRowNum();i++) {
            Row row=sheet.getRow(i); if (row==null || isBlank(row,h)) continue;
            Map<String,String> source=readSourceValues(row,h);
            List<String> issues=new ArrayList<>();
            String orderId=value(row,h,"ID COMPLETO DO PEDIDO");
            String storeId=value(row,h,"ID DA LOJA");
            String storeName=value(row,h,"NOME DA LOJA");
            String rawStatus=value(row,h,"STATUS FINAL DO PEDIDO");
            OffsetDateTime date=parseDateTime(cell(row,h,"DATA E HORA DO PEDIDO"));
            if (orderId.isBlank()) issues.add("ID completo do pedido ausente.");
            if (storeId.isBlank()) issues.add("ID da loja ausente.");
            if (date==null) issues.add("Data e hora do pedido inválidas.");
            if (rawStatus.isBlank()) issues.add("Status do pedido ausente.");
            String status=normalizeStatus(rawStatus);
            if ("UNKNOWN".equals(status)) issues.add("Status não reconhecido; o texto original será preservado.");
            ParsedOrderRow payload=new ParsedOrderRow(storeId,storeName,orderId,date,rawStatus,status,
                    value(row,h,"TURNO"),money(row,h,"VALOR DOS ITENS (R$)"),money(row,h,"TOTAL PAGO PELO CLIENTE (R$)"),
                    money(row,h,"TAXA DE ENTREGA PAGA PELO CLIENTE (R$)"),money(row,h,"INCENTIVO PROMOCIONAL DO IFOOD (R$)"),
                    money(row,h,"INCENTIVO PROMOCIONAL DA LOJA (R$)"),money(row,h,"INCENTIVO PROMOCIONAL DA REDE (R$)"),
                    money(row,h,"TAXA DE SERVIÇO (R$)"),money(row,h,"TAXAS E COMISSÕES (R$)"),money(row,h,"VALOR LÍQUIDO (R$)"),
                    value(row,h,"FORMA DE PAGAMENTO"),value(row,h,"TIPO DE ENTREGA"),value(row,h,"PRODUTO LOGÍSTICO"),value(row,h,"CANAL DE VENDA"),source);
            boolean valid=issues.stream().noneMatch(s->s.endsWith("ausente.")||s.endsWith("inválidas."));
            if (!valid) invalid++;
            if (date!=null) { LocalDate d=date.toLocalDate(); min=min==null||d.isBefore(min)?d:min; max=max==null||d.isAfter(max)?d:max; }
            if (valid) {
                gross=gross.add(z(payload.itemsValue())); paid=paid.add(z(payload.customerPaid()));
                ifoodPromo=ifoodPromo.add(z(payload.ifoodPromotion())); storePromo=storePromo.add(z(payload.storePromotion()));
                service=service.add(z(payload.serviceFee())); commissions=commissions.add(z(payload.feesAndCommissions())); net=net.add(z(payload.netReported()));
                switch(status){case "COMPLETED"->completed++;case "CANCELLED"->cancelled++;case "PARTIALLY_CANCELLED"->partial++;default->{}}
            }
            rows.add(new StagedReportRow(i+1,storeId + "|" + orderId,valid,false,false,issues,payload));
        }
        if(rows.isEmpty()) throw invalid("EMPTY_REPORT","O relatório não contém linhas de pedidos.");
        ImportSummary summary=new ImportSummary(rows.size(),completed,cancelled,partial,gross,paid,BigDecimal.ZERO,BigDecimal.ZERO,
                ifoodPromo,storePromo,service,commissions,net,0,invalid);
        return new ParsedReport(IntegrationReportType.ORDERS,min,max,rows,summary);
    }

    private ParsedReport parseIfoodMenu(Workbook workbook) {
        Sheet storeSheet = workbook.getSheet("Funil Loja");
        HeaderRow stores = menuHeader(storeSheet);
        if (stores == null || !stores.has("ID DA LOJA") || !stores.has("NOME DA LOJA"))
            throw invalid("UNSUPPORTED_REPORT", "A aba Funil Loja não informa os identificadores das lojas.");
        Map<String, String> storeIds = new HashMap<>();
        for (int i = stores.rowIndex() + 1; i <= storeSheet.getLastRowNum(); i++) {
            Row row = storeSheet.getRow(i); if (row == null) continue;
            String name = value(row, stores, "NOME DA LOJA"), id = value(row, stores, "ID DA LOJA");
            if (name.isBlank() || id.isBlank()) continue;
            String old = storeIds.putIfAbsent(name, id);
            if (old != null && !old.equals(id)) throw invalid("AMBIGUOUS_STORE", "O nome da loja aparece com identificadores diferentes no relatório.");
        }
        List<StagedReportRow> rows = new ArrayList<>();
        LocalDate reportDate = null;
        int rowNumber = 0;
        for (String sheetName : List.of("Itens", "Complementos")) {
            Sheet sheet = workbook.getSheet(sheetName);
            HeaderRow h = menuHeader(sheet);
            String nameColumn = sheetName.equals("Itens") ? "NOME DO ITEM" : "NOME DO COMPLEMENTO";
            if (h == null || !h.has("PERÍODO") || !h.has("NOME DA LOJA") || !h.has(nameColumn)
                    || !h.has("VENDAS TOTAL (QUANTIDADE)") || !h.has("VALOR TOTAL"))
                throw invalid("UNSUPPORTED_REPORT", "Faltam colunas obrigatórias na aba " + sheetName + ".");
            for (int i = h.rowIndex()+1; i<=sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i); if (row == null || isBlank(row,h)) continue;
                String period = value(row,h,"PERÍODO");
                String[] dates = period.split("\\s+-\\s+");
                if (dates.length != 2) throw invalid("INVALID_PERIOD", "Período inválido na aba " + sheetName + ".");
                LocalDate start, end;
                try { DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy"); start=LocalDate.parse(dates[0],f); end=LocalDate.parse(dates[1],f); }
                catch(DateTimeParseException ex) { throw invalid("INVALID_PERIOD", "Período inválido na aba " + sheetName + "."); }
                if (!start.equals(end)) throw invalid("DAILY_REPORT_REQUIRED", "Exporte o relatório de cardápio para um único dia. Períodos maiores não permitem identificar as quantidades por dia com segurança.");
                if (reportDate != null && !reportDate.equals(start)) throw invalid("INVALID_PERIOD", "O arquivo contém mais de um dia.");
                reportDate = start;
                String storeName = value(row,h,"NOME DA LOJA"), storeId = storeIds.get(storeName);
                String name = value(row,h,nameColumn);
                if (sheetName.equals("Complementos")) name = "[Complemento] " + name;
                BigDecimal quantity = money(row,h,"VENDAS TOTAL (QUANTIDADE)"), revenue = money(row,h,"VALOR TOTAL");
                List<String> issues = new ArrayList<>();
                if (storeId == null) issues.add("Loja não encontrada na aba Funil Loja.");
                if (value(row,h,nameColumn).isBlank()) issues.add("Nome do item ausente.");
                if (quantity == null || quantity.signum()<0) issues.add("Quantidade inválida.");
                if (revenue == null) issues.add("Valor inválido.");
                if (sheetName.equals("Complementos")) issues.add("Complemento agregado: confira combos antes de calcular estoque.");
                boolean valid = storeId != null && !value(row,h,nameColumn).isBlank() && quantity != null && quantity.signum()>=0 && revenue != null;
                Map<String,String> source = readSourceValues(row,h); source.put("ABA",sheetName);
                ParsedItemRow payload = new ParsedItemRow(storeId,storeName,start,name,normalizeName(name),revenue,quantity,source);
                rows.add(new StagedReportRow(++rowNumber,storeId+"|"+start+"|"+name,valid,false,false,issues,payload));
            }
        }
        if (rows.isEmpty()) throw invalid("EMPTY_REPORT", "O relatório de cardápio não contém vendas de itens no período.");
        return new ParsedReport(IntegrationReportType.ITEMS, reportDate, reportDate, rows,
                new ImportSummary(rows.size(),0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,
                        BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,0));
    }

    private HeaderRow menuHeader(Sheet sheet) {
        for (int i=0;i<=Math.min(sheet.getLastRowNum(),15);i++) {
            Row row=sheet.getRow(i); if(row==null) continue;
            Map<String,Integer> cols=new HashMap<>();
            for(Cell cell:row){String key=normalizeHeader(formatter.formatCellValue(cell));if(!key.isBlank())cols.put(key,cell.getColumnIndex());}
            if(cols.containsKey("PERIODO") && cols.containsKey("NOME DA LOJA")) return new HeaderRow(i,cols);
        }
        return null;
    }

    private ParsedReport parseFood99Orders(Sheet sheet, HeaderRow h) {
        List<StagedReportRow> rows = new ArrayList<>();
        LocalDate min = null, max = null;
        for (int i = h.rowIndex() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row, h)) continue;
            List<String> issues = new ArrayList<>();
            String storeId = value(row, h, "ID DO LOJA");
            String orderId = value(row, h, "ID DO PEDIDO");
            OffsetDateTime date = parseDateTime(cell(row, h, "HORÁRIO DO PEDIDO"));
            if (storeId.isBlank()) issues.add("ID da loja ausente.");
            if (orderId.isBlank()) issues.add("ID do pedido ausente.");
            if (date == null) issues.add("Data e hora do pedido inválidas.");
            String cancellation = value(row, h, "HORÁRIO DO CANCELAMENTO");
            String completion = value(row, h, "HORÁRIO DA CONCLUSÃO");
            String status = !cancellation.isBlank() && !completion.isBlank() ? "UNKNOWN" :
                    !cancellation.isBlank() ? "CANCELLED" : !completion.isBlank() ? "COMPLETED" : "UNKNOWN";
            String rawStatus = !cancellation.isBlank() && !completion.isBlank() ? "Cancelamento e conclusão informados; requer conferência" :
                    !cancellation.isBlank() ? "Cancelado" : !completion.isBlank() ? "Concluído" : "Não informado";
            if (!cancellation.isBlank()) issues.add("O relatório informa cancelamento; confira a situação no portal antes de considerar o pedido concluído.");
            if (completion.isBlank() && cancellation.isBlank()) issues.add("O relatório não informa conclusão ou cancelamento.");
            boolean valid = !storeId.isBlank() && !orderId.isBlank() && date != null;
            ParsedOrderRow payload = new ParsedOrderRow(storeId, value(row,h,"NOME DO ESTABELECIMENTO"), orderId, date, rawStatus, status, null,
                    money(row,h,"PREÇO ORIGINAL DO ITEM"), null, null, null, null, null, null,
                    sumWhenBothKnown(money(row,h,"DESPESAS DE COMISSÃO"), money(row,h,"TAXAS DE PROCESSAMENTO DE PAGAMENTO")),
                    money(row,h,"RECEITA REAL DA LOJA"), value(row,h,"FORMA DE PAGAMENTO"),
                    value(row,h,"MÉTODO DE ENTREGA"), null, null, readSourceValues(row,h));
            if (date != null) { LocalDate d = date.toLocalDate(); min = min == null || d.isBefore(min) ? d : min; max = max == null || d.isAfter(max) ? d : max; }
            rows.add(new StagedReportRow(i + 1, storeId + "|" + orderId, valid, false, false, issues, payload));
        }
        if (rows.isEmpty()) throw invalid("EMPTY_REPORT", "O relatório não contém linhas de pedidos.");
        return new ParsedReport(IntegrationReportType.ORDERS, min, max, rows,
                new ImportSummary(rows.size(),0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,
                        BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,0));
    }

    private ParsedReport parseItems(IntegrationProvider provider, Sheet sheet, HeaderRow h) {
        List<StagedReportRow> rows=new ArrayList<>(); LocalDate min=null,max=null;
        BigDecimal revenue=BigDecimal.ZERO,quantity=BigDecimal.ZERO; int invalid=0;
        for(int i=h.rowIndex()+1;i<=sheet.getLastRowNum();i++){
            Row row=sheet.getRow(i); if(row==null||isBlank(row,h))continue;
            Map<String,String> source=readSourceValues(row,h); List<String> issues=new ArrayList<>();
            String storeId=value(row,h,"ID DO LOJA"), storeName=value(row,h,"NOME DO ESTABELECIMENTO");
            String item=value(row,h,"NOME DO ITEM"); LocalDate date=parseDate(cell(row,h,"DATA"));
            BigDecimal itemRevenue=money(row,h,"RECEITA DO ITEM");
            BigDecimal sold=money(row,h,"VOLUME DE VENDAS DO ITEM");
            if(storeId.isBlank())issues.add("ID da loja ausente."); if(item.isBlank())issues.add("Nome do item ausente.");
            if(date==null)issues.add("Data da venda inválida."); if(itemRevenue==null)issues.add("Receita do item inválida.");
            if(sold==null||sold.signum()<0)issues.add("Volume de vendas inválido.");
            ParsedItemRow payload=new ParsedItemRow(storeId,storeName,date,item,normalizeName(item),itemRevenue,sold,source);
            boolean valid=issues.isEmpty(); if(!valid)invalid++;
            if(date!=null){min=min==null||date.isBefore(min)?date:min;max=max==null||date.isAfter(max)?date:max;}
            if(valid){revenue=revenue.add(itemRevenue);quantity=quantity.add(sold);}
            rows.add(new StagedReportRow(i+1,storeId+"|"+(date==null?"":date)+"|"+item,valid,false,false,issues,payload));
        }
        if(rows.isEmpty())throw invalid("EMPTY_REPORT","O relatório não contém linhas de vendas por item.");
        ImportSummary summary=new ImportSummary(rows.size(),0,0,0,BigDecimal.ZERO,BigDecimal.ZERO,revenue,quantity,
                BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,0,invalid);
        return new ParsedReport(IntegrationReportType.ITEMS,min,max,rows,summary);
    }

    private HeaderRow findHeader(Sheet sheet){
        for(int i=0;i<=Math.min(sheet.getLastRowNum(),15);i++){
            Row row=sheet.getRow(i);if(row==null)continue;Map<String,Integer> cols=new HashMap<>();
            for(Cell cell:row){String key=normalizeHeader(formatter.formatCellValue(cell));if(!key.isBlank())cols.put(key,cell.getColumnIndex());}
            if(cols.containsKey("ID COMPLETO DO PEDIDO") || (cols.containsKey("ID DO PEDIDO") && cols.containsKey("HORARIO DO PEDIDO")) || (cols.containsKey("NOME DO ITEM")&&cols.containsKey("RECEITA DO ITEM")&&cols.containsKey("VOLUME DE VENDAS DO ITEM"))) return new HeaderRow(i,cols);
        }
        return null;
    }
    private record HeaderRow(int rowIndex,Map<String,Integer> columns){boolean has(String key){return columns.containsKey(normalizeHeader(key));} int col(String key){return columns.getOrDefault(normalizeHeader(key),-1);}}
    private Cell cell(Row row,HeaderRow h,String name){int index=h.col(name);return index<0?null:row.getCell(index);}
    private String value(Row row,HeaderRow h,String name){Cell c=cell(row,h,name);return c==null?"":formatter.formatCellValue(c).trim();}
    private BigDecimal money(Row row,HeaderRow h,String name){Cell c=cell(row,h,name);if(c==null)return null;if(c.getCellType()==CellType.BLANK)return null;if(c.getCellType()==CellType.NUMERIC)return BigDecimal.valueOf(c.getNumericCellValue()).setScale(2,RoundingMode.HALF_UP);return parseDecimal(formatter.formatCellValue(c));}
    private BigDecimal parseDecimal(String raw){
        if(raw==null||raw.isBlank())return null;String s=raw.replace("R$","").replace("\u00a0","").replace(" ","").trim();
        if(s.contains(",")){s=s.replace(".","").replace(",",".");}
        try{return new BigDecimal(s).setScale(2,RoundingMode.HALF_UP);}catch(NumberFormatException ex){return null;}
    }
    private OffsetDateTime parseDateTime(Cell cell){
        if(cell==null)return null;
        if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell))return cell.getLocalDateTimeCellValue().atZone(STORE_ZONE).toOffsetDateTime();
        String value=formatter.formatCellValue(cell).trim(); if(value.isBlank())return null;
        try{return OffsetDateTime.parse(value);}catch(DateTimeParseException ignored){}
        for(DateTimeFormatter f:List.of(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))try{return LocalDateTime.parse(value,f).atZone(STORE_ZONE).toOffsetDateTime();}catch(DateTimeParseException ignored){}
        LocalDate d=parseDate(cell);return d==null?null:d.atStartOfDay(STORE_ZONE).toOffsetDateTime();
    }
    private LocalDate parseDate(Cell cell){
        if(cell==null)return null;if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell))return cell.getLocalDateTimeCellValue().toLocalDate();
        String value=formatter.formatCellValue(cell).trim();if(value.isBlank())return null;
        for(DateTimeFormatter f:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("dd/MM/yyyy"),DateTimeFormatter.ofPattern("d/M/yyyy")))try{return LocalDate.parse(value,f);}catch(DateTimeParseException ignored){}
        return null;
    }
    private boolean isBlank(Row row,HeaderRow h){for(int i:h.columns().values()){Cell c=row.getCell(i);if(c!=null&&!formatter.formatCellValue(c).isBlank())return false;}return true;}
    private Map<String,String> readSourceValues(Row row,HeaderRow h){Map<String,String> values=new LinkedHashMap<>();h.columns().forEach((name,index)->values.put(name,formatter.formatCellValue(row.getCell(index))));return values;}
    public static String normalizeHeader(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toUpperCase(Locale.ROOT).replaceAll("\\s+"," ").trim();}
    public static String normalizeName(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();}
    private String normalizeStatus(String raw){String s=normalizeHeader(raw);if(s.equals("CONCLUIDO")||s.equals("COMPLETED"))return "COMPLETED";if(s.equals("CANCELADO")||s.equals("CANCELLED"))return "CANCELLED";if(s.equals("CANCELAMENTO PARCIAL"))return "PARTIALLY_CANCELLED";return "UNKNOWN";}
    private BigDecimal z(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private BigDecimal sumWhenBothKnown(BigDecimal first,BigDecimal second){return first==null||second==null?null:first.add(second);}

    private byte[] unzipWorkbook(byte[] archive){
        Path tempFile=null;
        try{
            tempFile=Files.createTempFile("dafabi-report-",".zip");
            Files.write(tempFile,archive);
            byte[] workbook=null;int entries=0;
            try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(tempFile.toFile(),StandardCharsets.UTF_8)){
                Enumeration<? extends ZipEntry> iterator=zip.entries();
                while(iterator.hasMoreElements()){
                    ZipEntry entry=iterator.nextElement();if(entry.isDirectory())continue;
                    if(++entries>5)throw invalid("INVALID_ARCHIVE","O ZIP contém arquivos demais.");
                    if(!entry.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx"))continue;
                    if(workbook!=null)throw invalid("INVALID_ARCHIVE","O ZIP deve conter apenas uma planilha XLSX.");
                    if(entry.getSize()>MAX_UNCOMPRESSED_BYTES)throw invalid("FILE_TOO_LARGE","O arquivo compactado excede o limite de leitura.");
                    try(InputStream input=zip.getInputStream(entry);ByteArrayOutputStream output=new ByteArrayOutputStream()){
                        byte[] buffer=new byte[8192];int read,total=0;
                        while((read=input.read(buffer))!=-1){total+=read;if(total>MAX_UNCOMPRESSED_BYTES)throw invalid("FILE_TOO_LARGE","O arquivo compactado excede o limite de leitura.");output.write(buffer,0,read);}
                        workbook=output.toByteArray();
                    }
                }
            }
            if(workbook==null)throw invalid("INVALID_ARCHIVE","O ZIP não contém uma planilha XLSX.");
            return workbook;
        }catch(IOException ex){throw invalid("INVALID_ARCHIVE","Não foi possível abrir o ZIP enviado.");}
        finally{if(tempFile!=null)try{Files.deleteIfExists(tempFile);}catch(IOException ignored){}}
    }
    private BusinessException invalid(String code,String message){return new BusinessException(code,message,HttpStatus.BAD_REQUEST);}
}
