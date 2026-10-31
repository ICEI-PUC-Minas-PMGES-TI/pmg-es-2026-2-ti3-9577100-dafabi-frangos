package com.dafabi.integrations;

import com.dafabi.integrations.application.*;
import com.dafabi.integrations.domain.*;
import com.dafabi.shared.exception.BusinessException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.assertj.core.api.Assertions.*;

class MarketplaceReportParserTest {
    private final MarketplaceReportParser parser = new MarketplaceReportParser();
    private static void row(Sheet sheet, int index, String... values) {
        Row row=sheet.createRow(index);
        for(int i=0;i<values.length;i++) row.createCell(i).setCellValue(values[i]);
    }
    private byte[] bytes(XSSFWorkbook workbook) throws Exception {
        try(workbook; ByteArrayOutputStream out=new ByteArrayOutputStream()) { workbook.write(out); return out.toByteArray(); }
    }
    @Test void readsReal99ReportWhenProvided() throws Exception {
        String path=System.getenv("DAFABI_99_ORDER_SAMPLE");
        org.junit.jupiter.api.Assumptions.assumeTrue(path != null && !path.isBlank());
        ParsedReport report=parser.parse(IntegrationProvider.FOOD_99, "sample.zip", java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)));
        assertThat(report.reportType()).isEqualTo(IntegrationReportType.ORDERS);
        assertThat(report.rows()).hasSize(13).allMatch(StagedReportRow::valid);
        assertThat(report.rows().stream().map(r -> (ParsedOrderRow)r.payload()).map(ParsedOrderRow::itemsValue)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)).isEqualByComparingTo("733.60");
    }
    @Test void reads99OrdersAndPreservesAmbiguousCancellationAndLongIds() throws Exception {
        XSSFWorkbook w=new XSSFWorkbook(); Sheet s=w.createSheet();
        row(s,0,"ID do loja","Nome do estabelecimento","ID do pedido","Horário do pedido","Horário da conclusão","Horário do cancelamento","Preço original do item","Receita real da loja","Forma de pagamento");
        row(s,1,"5764609852534688112","Loja","5764696717094093306","2026-10-25 14:20:35","2026-10-25 14:50:27","","67,10","43,97","Pagamento online");
        row(s,2,"5764609852534688112","Loja","5764696717094093307","2026-10-25 14:20:35","2026-10-25 14:50:27","2026-10-25 14:50:27","10,00","0","Dinheiro");
        byte[] xlsx=bytes(w); ByteArrayOutputStream zip=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(zip)){z.putNextEntry(new ZipEntry("report.xlsx"));z.write(xlsx);z.closeEntry();}
        ParsedReport report=parser.parse(IntegrationProvider.FOOD_99,"report.zip",zip.toByteArray());
        assertThat(report.reportType()).isEqualTo(IntegrationReportType.ORDERS);
        ParsedOrderRow first=(ParsedOrderRow)report.rows().get(0).payload();
        assertThat(first.externalOrderId()).isEqualTo("5764696717094093306");
        assertThat(first.itemsValue()).isEqualByComparingTo("67.10");
        assertThat(first.normalizedStatus()).isEqualTo("COMPLETED");
        ParsedOrderRow cancelled=(ParsedOrderRow)report.rows().get(1).payload();
        assertThat(cancelled.normalizedStatus()).isEqualTo("UNKNOWN");
        assertThat(report.rows().get(1).issues()).isNotEmpty();
    }
    private XSSFWorkbook menu(String period) {
        XSSFWorkbook w=new XSSFWorkbook(); Sheet stores=w.createSheet("Funil Loja");
        row(stores,0,"Período","Id da Loja","Nome da Loja"); row(stores,1,period,"1234","Loja");
        Sheet items=w.createSheet("Itens");
        row(items,0,"Período","Nome da Loja","Nome do item","Vendas total (quantidade)","Valor Total");
        row(items,1,period,"Loja","Combo","2","100");
        Sheet complements=w.createSheet("Complementos");
        row(complements,0,"Período","Nome da Loja","Nome do complemento","Vendas total (quantidade)","Valor total");
        row(complements,1,period,"Loja","Pudim","2","0");
        return w;
    }
    @Test void readsDailyMenuAndSeparatesComplements() throws Exception {
        ParsedReport r=parser.parse(IntegrationProvider.IFOOD,"menu.xlsx",bytes(menu("25/10/2026 - 25/10/2026")));
        assertThat(r.start()).isEqualTo(LocalDate.of(2026,10,25));
        assertThat(r.rows()).hasSize(2);
        ParsedItemRow complement=(ParsedItemRow)r.rows().get(1).payload();
        assertThat(complement.externalStoreId()).isEqualTo("1234");
        assertThat(complement.externalItemName()).isEqualTo("[Complemento] Pudim");
        assertThat(r.rows().get(1).issues()).isNotEmpty();
    }
    @Test void refusesPeriodAggregateThatCannotBeAttributedToOneDay() throws Exception {
        byte[] workbook=bytes(menu("20/10/2026 - 26/10/2026"));
        assertThatThrownBy(()->parser.parse(IntegrationProvider.IFOOD,"menu.xlsx",workbook))
                .isInstanceOf(BusinessException.class).extracting("code").isEqualTo("DAILY_REPORT_REQUIRED");
    }
}
